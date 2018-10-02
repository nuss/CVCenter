CVCenterKeyboard {
	classvar <all;
	var <synthDefName, <keyboardArg, <velocArg, <bendArg, <widgetsPrefix;
	var <>bendSpec;
	var on, off, bend, namesCVs;
	var <>debug = false;

	*new { |synthDefName, keyboardArg = \freq, velocArg = \veloc, bendArg = \bend, widgetsPrefix = \kb, connectMidi = true|
		^super.newCopyArgs(synthDefName, keyboardArg, velocArg, bendArg, widgetsPrefix).init(connectMidi);
	}

	init { |connectMidi|
		synthDefName = synthDefName.asSymbol;

		SynthDescLib.at(synthDefName) ?? {
			Error(
				"The SynthDef '%' does not exist".format(synthDefName)
			).throw;
		};

		if (SynthDescLib.at(synthDefName).hasGate.not) {
			Error(
				"The given SynthDef does not provide a 'gate' argument and can not be used."
			).throw;
		};

		all ?? {
			all = ();
		};

		all.put(synthDefName, this);

		if (CVCenter.scv[synthDefName].isNil) {
			CVCenter.scv.put(synthDefName, Array.newClear(128));
			MIDIClient.init;
			// doesn't seem to work properly on Ubuntustudio 16
			// possibly has to be done manually in QJackQtl...
			if (connectMidi) {
				try { MIDIIn.connectAll } { |error|
					error.postln;
					"MIDIIn.connectAll failed. Please establish the necessary connections manually".warn;
				}
			}
		} {
			"A keyboard for the SynthDef '%' has already been initialized".format(synthDefName).warn;
		}
	}

	// keyboardArg is the arg that will be set through playing the keyboard
	// bendArg will be the arg that's set through the pitch bend wheel
	setUpControls { |tab, prefix, keyboardControl, velocControl, bendControl, server, deactivateDefaultWidgetActions = true|
		var testSynth, notesEnv;
		var args = [];

		server ?? { server = Server.default };

		prefix !? { widgetsPrefix = prefix };
		keyboardControl !? { keyboardArg = keyboardControl };
		velocControl !? { velocArg = velocControl };
		bendControl !? { bendArg = bendControl };

		tab ?? { tab = synthDefName };

		server.waitForBoot {
			// SynthDef *should* have an \amp arg, otherwise it will sound for moment
			testSynth = Synth(synthDefName);
			// \gate will be set internally
			testSynth.cvcGui(prefix: widgetsPrefix, excemptArgs: [keyboardArg, velocArg, \gate], tab: tab, completionFunc: {
				this.prAddWidgetActionsForKeyboard(deactivateDefaultWidgetActions);
			});
			testSynth.release;
		}
	}

	// private
	prAddWidgetActionsForKeyboard { |deactivateDefaultActions|
		var args = SynthDescLib.at(synthDefName).controlDict.keys.asArray;
		var wdgtNames, wdgtName, nameString;

		wdgtNames = this.prInitCVs(args);

		args.do { |name, i|
			wdgtName = wdgtNames[i];
			CVCenter.cvWidgets[wdgtName] !? {
				if (CVCenter.cvWidgets[wdgtName].class == CVWidget2D) {
					#[lo, hi].do { |slot|
						CVCenter.addActionAt(
							wdgtName, 'keyboard set arg',
							"{ |cv| CVCenter.scv['%'].do { |synth| synth !? { synth.set('%', cv.value) }}; }"
							.format(synthDefName, name), slot);
						CVCenter.activateActionAt(wdgtName, \default, deactivateDefaultActions.not, slot);
					}
				} {
					CVCenter.addActionAt(wdgtName, 'keyboard set arg',
						"{ |cv| CVCenter.scv['%'].do { |synth| synth !? { synth.set('%', cv.value) }}; }"
						.format(synthDefName, name));
					CVCenter.activateActionAt(wdgtName, \default, deactivateDefaultActions.not);
				}
			}
		};

		this.prInitKeyboard;
	}

	reInit {
		var args = SynthDescLib.at(synthDefName).controlDict.keys.asArray;
		CVCenter.scv[synthDefName] !? {
			"re-initializing!".postln;
			this.free;
			CVCenter.scv.put(synthDefName, Array.newClear(128));
			this.prInitCVs(args);
			this.prInitKeyboard;
		}
	}

	// private
	prInitCVs { |args|
		var nameString, wdgtName;
		var wdgtNames = [];

		namesCVs = [];

		args.do { |name|
			nameString = name.asString;
			widgetsPrefix.notNil !? {
				nameString = nameString[0].toUpper ++ nameString[1..nameString.size-1];
			};
			wdgtName = (widgetsPrefix ++ nameString).asSymbol;
			CVCenter.cvWidgets[wdgtName] !? {
				if (CVCenter.cvWidgets[wdgtName].class == CVWidget2D) {
					if (namesCVs.includes(name).not) {
						namesCVs = namesCVs.add(name).add(CVCenter.at(wdgtName).asArray);
					}
				} {
					if (namesCVs.includes(name).not) {
						namesCVs = namesCVs.add(name).add(CVCenter.at(wdgtName));
					}
				}
			};
			wdgtNames = wdgtNames.add(wdgtName);
		};

		^wdgtNames;
	}

	// private
	prInitKeyboard {
		on = MIDIFunc.noteOn({ |veloc, num, chan, src|
			var argsValues = [keyboardArg, num.midicps, velocArg, veloc * 0.005] ++ namesCVs.deepCollect(2, _.value);
			if (this.debug) { "on[num: %]: %\n".postf(num, argsValues) };
			CVCenter.scv[synthDefName][num] = Synth(synthDefName, argsValues);
		});

		off = MIDIFunc.noteOff({ |veloc, num, chan, src|
			if (this.debug) { "off[num: %]\n".postf(num) };
			CVCenter.scv[synthDefName][num].release;
		});

		bend = MIDIFunc.bend({ |bendVal, chan, src|
			if (this.debug) { "bend: %\n".postf(bendVal) };
			CVCenter.scv[synthDefName].do({ |synth, i|
				synth.set(bendArg, (i + bendSpec.map(bendVal / 16383)).midicps)
			})
		});
	}

	free {
		on !? { on.free };
		off !? { off.free };
		bend !? { bend.free };
		CVCenter.scv[synthDefName].do(_.release);
		CVCenter.scv.removeAt(synthDefName);
	}
}