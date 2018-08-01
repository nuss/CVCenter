+CVCenter {

	*midiKeyboardGated { |synthDef = \default, specs, tab, server|
		var testSynth;

		server ?? { server = Server.default };
		if (server.serverRunning.not) {
			Error(
				"The server '%' must already be running when invoking CVCenter:*midiKeyboard".format(server)
			).throw;
		};

		SynthDescLib.at(synthDef) ?? {
			Error(
				"The SynthDef '%' does not exist".format(synthDef)
			).throw;
		};

		if (SynthDescLib.at(synthDef).hasGate.not) {
			Error(
				"The given SynthDef does not provide a 'gate' argument and can not be used."
			).throw;
		}

		testSynth = Synth(synthDef, [\amp, 0]);
	}

}