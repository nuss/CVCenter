KeyDownActions {

	// classvar <allEditors;
	// classvar <viewActions;
	classvar <>keyCodes, <>modifiersQt, <>modifiersCocoa, <>arrowsModifiersQt, <>arrowsModifiersCocoa;
	// var <window, <>actions;

	*initClass {
		Class.initClassTree(Platform);
		Class.initClassTree(GUI);

		// viewActions = IdentityDictionary.new;

		Platform.case(
			\linux, {
				this.keyCodes = IdentityDictionary[
					$1 ->				10,
					$2 ->				11,
					$3 ->				12,
					$4 ->				13,
					$5 ->				14,
					$6 ->				15,
					$7 ->				16,
					$8 ->				17,
					$9 ->				18,
					$0 ->				19,
					$- ->				20,
					$= ->				21,
					$q ->				24,
					$w ->				25,
					$e ->				26,
					$r ->				27,
					$t ->				28,
					$y ->				29,
					$u ->				30,
					$i ->				31,
					$o ->				32,
					$p ->				33,
					$[ ->				34,
					$] ->				35,
					$a ->				38,
					$s ->				39,
					$d ->				40,
					$f ->				41,
					$g ->				42,
					$h ->				43,
					$j ->				44,
					$k ->				45,
					$l ->				46,
					$; ->				47,
					$' ->				48,
					(92.asAscii) ->		51,
					$< ->				94,
					$z ->				52,
					$x ->				53,
					$c ->				54,
					$v ->				55,
					$b ->				56,
					$n ->				57,
					$m ->				58,
					$, ->				59,
					$. ->				60,
					$/ ->				61,
					\esc ->				9,
					$` ->				49,
					'arrow up' ->		111,
					'arrow down' ->		116,
					'arrow left' ->		113,
					'arrow right' ->	114,
				];

				// arrowsModifiers = IdentityDictionary[];

				this.modifiersQt = IdentityDictionary[
					\none ->			0,
					\shift ->			131072,
					\alt ->				524288,
					'alt + shift' ->	655360,
				];

				this.modifiersCocoa = this.modifiersQt;
				this.arrowsModifiersQt = this.modifiersQt;
			},

			\osx, {
				this.keyCodes = IdentityDictionary[
					$1 -> 				18,
					$2 -> 				19,
					$3 -> 				20,
					$4 -> 				21,
					$5 -> 				23,
					$6 -> 				22,
					$7 -> 				26,
					$8 -> 				28,
					$9 -> 				25,
					$0 -> 				29,
					$- -> 				27,
					$= -> 				24,
					$q -> 				12,
					$w -> 				13,
					$e -> 				14,
					$r -> 				15,
					$t -> 				17,
					$y -> 				16,
					$u -> 				32,
					$i -> 				34,
					$o -> 				31,
					$p -> 				35,
					$[ -> 				33,
					$] -> 				30,
					$a -> 				0,
					$s -> 				1,
					$d -> 				2,
					$f -> 				3,
					$g -> 				5,
					$h -> 				4,
					$j -> 				38,
					$k -> 				40,
					$l -> 				37,
					$; -> 				41,
					$' -> 				39,
					(92.asAscii) -> 	42,
					$` -> 				50,
					$z -> 				6,
					$x -> 				7,
					$c -> 				8,
					$v -> 				9,
					$b -> 				11,
					$n -> 				45,
					$m -> 				46,
					$, -> 				43,
					$. -> 				47,
					$/ -> 				44,
					\space ->			49,
					\esc -> 			53,
					'arrow up' -> 		126,
					'arrow down' -> 	125,
					'arrow left' -> 	123,
					'arrow right' -> 	124,
				];

				this.arrowsModifiersCocoa = IdentityDictionary[
					'none' ->			10486016,
					'alt' ->			11010336,
					'shift' ->			10617090,
					'alt + shift' ->	11141410
				];

				this.modifiersCocoa = IdentityDictionary[
					\none ->			0,
					\alt ->				524576,
					\shift ->			131330,
					'alt + shift' ->	655650,
				];

				this.arrowsModifiersQt = IdentityDictionary[
					\none ->			2097152,
					'alt' ->			2621440,
					'shift' ->			2228224,
					'alt + shift' ->	2752512
				];

				this.modifiersQt = IdentityDictionary[
					\none ->			0,
					\shift ->			131072,
					\alt ->				524288,
					'alt + shift' ->	655360,
				]
			},

			\windows, {
				this.keyCodes = IdentityDictionary[
					$1 -> 				49,
					$2 -> 				50,
					$3 -> 				51,
					$4 -> 				52,
					$5 -> 				53,
					$6 -> 				54,
					$7 -> 				55,
					$8 -> 				56,
					$9 -> 				57,
					$0 -> 				48,
					$- -> 				189,
					$= -> 				187,
					$q -> 				81,
					$w -> 				87,
					$e -> 				69,
					$r -> 				82,
					$t -> 				84,
					$y -> 				89,
					$u -> 				85,
					$i -> 				73,
					$o -> 				79,
					$p -> 				80,
					$[ -> 				219,
					$] -> 				221,
					$a -> 				65,
					$s -> 				83,
					$d -> 				68,
					$f -> 				70,
					$g -> 				71,
					$h -> 				72,
					$j -> 				74,
					$k -> 				75,
					$l -> 				76,
					$; -> 				186,
					$' -> 				222,
					(92.asAscii) ->		220,
					$< -> 				226,
					$z -> 				90,
					$x -> 				88,
					$c -> 				67,
					$v -> 				86,
					$b -> 				66,
					$n -> 				78,
					$m -> 				77,
					$, -> 				188,
					$. -> 				190,
					$/ -> 				191,
					\esc -> 			27,
					\space -> 			65,
					$` -> 				192,
					'arrow up' -> 		38,
					'arrow down' -> 	40,
					'arrow left' -> 	37,
					'arrow right' -> 	39,
				];

				this.modifiersQt = IdentityDictionary[
					\none ->			0,
					\shift -> 			131072,
					\alt -> 			524288,
					'alt + shift' -> 	655360,
				];

				this.modifiersCocoa = this.modifiersQt;
				this.arrowsModifiersQt = this.modifiersQt;
			},
			{
				// dummies for unknown platforms
				this.keyCodes = IdentityDictionary.new;
				this.modifiersQt = IdentityDictionary.new;
				this.arrowsModifiersQt = IdentityDictionary.new;
				this.modifiersCocoa = IdentityDictionary.new;
				this.arrowsModifiersCocoa = IdentityDictionary.new;
			}
		)
	}

}

KeyDownActionsEditor : KeyDownActions {

	classvar all;
	var <window, <tmpShortcuts, <shortcutFields, <shortcutTexts, <funcFields, <editAreas, <editButs, cachedScrollViewSC;

	*initClass {
		all = List.new;
	}

	*new { |parent, name, bounds, shortcutsDict, save=true, closeOnSave=false|
		^super.new.init(parent, name, bounds, shortcutsDict, save, closeOnSave);
	}

	init { |parent, name, bounds, shortcutsDict, save, closeOnSave|
		var scrollArea, scrollView, butArea, newBut, saveBut;
		var removeButs, makeEditArea;
		var scrollFlow, editFlows, butFlow;
		var editAreasBg, staticTextColor, staticTextFont, shortCutFont, textFieldFont;
		var order, orderedShortcuts;
		var tmpEditFlow, tmpIndex, join = " + ", mods;
		// vars for makeEditArea
		var count, rmBounds;
		var thisArrowsModifiers, thisModifiers;

		Platform.case(
			\osx, {
				switch(GUI.id,
					\qt, {
						thisModifiers = modifiersQt;
						thisArrowsModifiers = arrowsModifiersQt;
					},
					\cocoa, {
						thisModifiers = modifiersCocoa;
						thisArrowsModifiers = arrowsModifiersCocoa;
					}
				)
			},
			{
				thisModifiers = modifiersQt;
				thisArrowsModifiers = arrowsModifiersQt;
			}
		);

		editAreasBg = Color(0.8, 0.8, 0.8);
		staticTextColor = Color(0.1, 0.1, 0.1);
		staticTextFont = Font("Arial", 10);
		shortCutFont = Font("Arial", 12, true);
		textFieldFont = Font("Andale Mono", 10);

		// if(name.isNil) { thisName = view.class } { thisName = name };

		if(parent.isNil) {
			window = Window("shortcut editor:"+name, bounds ?? { Rect(
				Window.screenBounds.width-600/2,
				Window.screenBounds.height-600/2,
				600, 600
			) });
			// view.onClose_({ window.close });
		} { window = parent };

		window.onClose_({
			all.remove(this);
			cachedScrollViewSC !? {
				ScrollView.globalKeyDownAction_(cachedScrollViewSC);
			}
		});

		#editAreas, editFlows, shortcutTexts, shortcutFields, editButs, removeButs, funcFields, tmpShortcuts = []!8;

		scrollArea = ScrollView(window.asView, Rect(
			0, 0, window.asView.bounds.width, window.asView.bounds.height-23
		)).hasHorizontalScroller_(false).background_(editAreasBg).hasBorder_(false);
		butArea = CompositeView(window.asView, Rect(
			0, window.asView.bounds.height-23, window.asView.bounds.width, 23
		)).background_(editAreasBg);

		scrollView = CompositeView(scrollArea, Rect(
			0, 0, scrollArea.bounds.width, scrollArea.bounds.height
		));

		scrollView.decorator = scrollFlow = FlowLayout(scrollView.bounds, 0@0, 1@0);

		makeEditArea = { |shortcut, funcString|
			var editArea, shortcutText, shortcutField, editBut, removeBut, funcField;

			// "shortcut: %, funcString: %, scrollView.parent: %\n".postf(shortcut, funcString, scrollView.parent);

			editAreas = editAreas.add(
				editArea = CompositeView(
					scrollView, scrollFlow.bounds.width-16@100
				).background_(editAreasBg)
			);

			count = editAreas.size-1;

			scrollView.bounds_(Rect(
				scrollView.bounds.left,
				scrollView.bounds.top,
				scrollView.bounds.width,
				count+1 * 100
			));

			editArea.decorator = tmpEditFlow = FlowLayout(editArea.bounds, 7@7, 2@2);

			tmpShortcuts = tmpShortcuts.add(nil);

			shortcutTexts = shortcutTexts.add(
				shortcutText = StaticText(editArea, 40@15)
					.string_("shortcut:")
					.font_(staticTextFont)
					.stringColor_(staticTextColor)
					.canFocus_(false)
				;
			);

			shortcutFields = shortcutFields.add(
				shortcutField = StaticText(editArea, tmpEditFlow.indentedRemaining.width-125@15)
					.background_(Color.white)
					.font_(shortCutFont)
					.stringColor_(staticTextColor)
					.canFocus_(false)
				;
			);

			shortcut !? {
				shortcutField.string_(" "++shortcut);
			};

			editButs = editButs.add(
				editBut = Button(editArea, 60@15)
					.states_([
						["edit", staticTextColor],
						["end edit", staticTextColor]
					])
					.font_(staticTextFont)
					.action_({ |bt|
						switch(bt.value,
							1, {
								all.do({ |ed|
									ed.editAreas.do(_.background_(editAreasBg));
									ed.editButs.do(_.value_(0));
									ed.shortcutTexts.do(_.stringColor_(staticTextColor));
									ed.funcFields.do(_.enabled_(false));
								});
								editBut.value_(1);
								cachedScrollViewSC = ScrollView.globalKeyDownAction;
								ScrollView.globalKeyDownAction_({ |view, char, mod, unicode, keycode, key|
									// [view, char, mod, unicode, keycode, key].postcs;
									// GUI.id.postln;
									if(keyCodes.findKeyForEqualValue(keycode).notNil, {
										char !? {
											if(thisModifiers.includes(mod) and:{
												thisModifiers.findKeyForValue(mod) != \none
											}, {
												mods = thisModifiers.findKeyForValue(mod);
											}, {
												if(thisArrowsModifiers.includes(mod) and:{
													thisArrowsModifiers.findKeyForValue(mod) != \none
												}, {
													mods = thisArrowsModifiers.findKeyForValue(mod);
												})
											});
											if(mod.notNil and:{
												mod != thisModifiers[\none] and:{
													mod != thisArrowsModifiers[\none]
												}
											}, {
												// "tmpShortcuts[%]: %\n".postf(count, tmpShortcuts[count]);
												shortcutField.string_(
													" "++ mods ++ join ++
													keyCodes.findKeyForValue(keycode)
												);
												if(thisArrowsModifiers.includes(mod), {
													if(GUI.id !== \cocoa, {
														tmpShortcuts[count] = (mods ++ join ++ keyCodes.findKeyForValue(keycode)).asSymbol -> (
															func: funcField.string,
															keyCode: keycode,
															arrowModifierCocoa: nil,
															arrowModifierQt: mod,
															modifierCocoa: nil,
															modifierQt: nil
														)
													}, {
														tmpShortcuts[count] = (mods ++ join ++ keyCodes.findKeyForValue(keycode)).asSymbol -> (
															func: funcField.string,
															keyCode: keycode,
															arrowModifierCocoa: mod,
															arrowModifierQt: nil,
															modifierCocoa: nil,
															modifierQt: nil
														)
													});
												}, {
													if(GUI.id !== \cocoa, {
														tmpShortcuts[count] = (mods ++ join ++ keyCodes.findKeyForValue(keycode)).asSymbol -> (
															func: funcField.string,
															keyCode: keycode,
															arrowModifierCocoa: nil,
															arrowModifierQt: nil,
															modifierCocoa: nil,
															modifierQt: mod
														)
													}, {
														tmpShortcuts[count] = (mods ++ join ++ keyCodes.findKeyForValue(keycode)).asSymbol -> (
															func: funcField.string,
															keyCode: keycode,
															arrowModifierCocoa: nil,
															arrowModifierQt: nil,
															modifierCocoa: mod,
															modifierQt: nil
														)
													})
												});
												// "tmpShortcuts[%] =  new: %\n".postf(count, tmpShortcuts[count]);
											}, {
												shortcutField.string_(
													" "++
													keyCodes.findKeyForValue(keycode)
												);
												tmpShortcuts[count] = keyCodes.findKeyForValue(keycode) -> (
													func: funcField.string,
													keyCode: keycode,
													arrowModifierCocoa: nil,
													arrowModifierQt: nil,
													modifierCocoa: nil,
													modifierQt: nil
												);
											})
										}
									})
								});
								funcField.enabled_(true);
								editArea.background_(Color.red);
								shortcutText.stringColor_(Color.white);
							},
							0, {
								ScrollView.globalKeyDownAction_(cachedScrollViewSC);
								funcField.enabled_(false);
								editArea.background_(editAreasBg);
								shortcutText.stringColor_(staticTextColor);
							}
						)
					})
				;
			);

			removeButs = removeButs.add(
				removeBut = Button(editArea, 60@15)
					.states_([["remove", staticTextColor]])
					.action_({ |bt|
						tmpIndex = editAreas.detectIndex({ |it| it == bt.parent });
						rmBounds = bt.parent.bounds;
						bt.parent.remove;
						editAreas.removeAt(tmpIndex);
						tmpShortcuts.remove(tmpShortcuts[tmpIndex]);
						editAreas.do({ |it|
							if(it.bounds.top > rmBounds.top, {
								it.bounds_(Rect(
									it.bounds.left,
									it.bounds.top-100,
									it.bounds.width,
									it.bounds.height
								))
							})
						});
						scrollView.bounds_(Rect(
							scrollView.bounds.left,
							scrollView.bounds.top,
							scrollView.bounds.width,
							scrollView.bounds.height-100
						))
					})
					.font_(staticTextFont)
				;
			);
			tmpEditFlow.nextLine;
			funcFields = funcFields.add(
				funcField = TextView(
					editArea,
					tmpEditFlow.indentedRemaining.width@tmpEditFlow.indentedRemaining.height
				).font_(textFieldFont).enabled_(false).syntaxColorize.action_({ |ffield|
					tmpShortcuts[count].value.func = funcField.string;
				});
			);
			funcString !? { funcFields[count].string_(funcString) };
		};

		order = shortcutsDict.order;

		order.do({ |shortcut, i|
			makeEditArea.(shortcut, shortcutsDict[shortcut][\func].replace("\t", " "));
			tmpShortcuts[i] = shortcut -> (
				func: shortcutsDict[shortcut][\func],
				keyCode: shortcutsDict[shortcut][\keyCode],
				modifierQt: shortcutsDict[shortcut][\modifierQt],
				modifierCocoa: shortcutsDict[shortcut][\modifierCocoa],
				arrowModifierQt: shortcutsDict[shortcut][\arrowModifierQt],
				arrowModifierCocoa: shortcutsDict[shortcut][\arrowModifierCocoa]
			)
		});

		// tmpShortcuts.do({ |it, i|
		// 	"tmpShortcuts[%]: %\n".postf(i, it);
		// 	"funcFields[%].string: %\n".postf(i, funcFields[i].string);
		// });

		butArea.decorator = butFlow = FlowLayout(butArea.bounds, 7@4, 3@0);

		newBut = Button(butArea, 70@15)
			.font_(staticTextFont)
			.states_([["new action", staticTextColor]])
			.action_({ |bt|
				editAreas.do({ |cview|
					cview.bounds_(Rect(
						cview.bounds.left,
						cview.bounds.top+100,
						cview.bounds.width,
						cview.bounds.height
					));
				});
				scrollFlow.reset;
				makeEditArea.(funcString: "{ /*do something */ }");
			})
		;
		parent ?? { window.front };
		all.add(this);
	}

	result {
		var res;
		res = IdentityDictionary.new;
		tmpShortcuts.do({ |it| res.put(it.key, it.value) });
		^res;
	}

}

KeyCodesEditor : KeyDownActions {

	classvar all;
	var <window, <eas;

	*initClass {
		all = List.new;
	}

	*new { |parent, bounds, closeOnSave=false|
		^super.new.init(parent, bounds, closeOnSave);
	}

	init { |parent, bounds, closeOnSave|
		var platform, scrollArea, scrollView, flow;
		var editAreasBg, staticTextColor, staticTextFont, shortCutFont, textFieldFont;
		var makeEditArea, editArea;

		editAreasBg = Color(0.8, 0.8, 0.8);
		staticTextColor = Color(0.1, 0.1, 0.1);
		staticTextFont = Font("Arial", 10);
		shortCutFont = Font("Arial", 12, true);
		textFieldFont = Font("Andale Mono", 10);

		Platform.case(
			\osx, { platform = "OSX" },
			\linux, { platform = "Linux" },
			\windows, { platform = "Windows" },
			{ platform = "an unknown platform" }
		);

		switch(GUI.id,
			\cocoa, { platform = platform+"[Cocoa]" },
			\qt, { platform = platform+"[Qt]" },
			\swing, { platform = platform+"[SwingOSC]" }
		);

		if(parent.isNil) {
			window = Window("key-codes and modifiers for"+platform, bounds ?? { Rect(
				Window.screenBounds.width-600/2,
				Window.screenBounds.height-600/2,
				600, 600
			) });
		} { window = parent };

		window.onClose_({ all.remove(this) });

		scrollArea = ScrollView(window.view, Point(
			window.view.bounds.width, window.view.bounds.height
		)).hasHorizontalScroller_(false).hasVerticalScroller_(true).background_(editAreasBg).hasBorder_(false);

		scrollView = CompositeView(scrollArea, Point(
			scrollArea.bounds.width, scrollArea.bounds.height
		));

		scrollView.decorator = flow = FlowLayout(scrollView.bounds, 8@8, 0@0);

		makeEditArea = { |dictName, dict, height|
			var editArea, name, key;

			name = StaticText(scrollView, Point(
				flow.indentedRemaining.width, 17
			)).font_(shortCutFont).stringColor_(staticTextColor).string_(dictName);

			flow.nextLine;
			editArea = TextView(scrollView, Point(
				flow.indentedRemaining.width-15, height
			)).font_(textFieldFont).syntaxColorize.hasVerticalScroller_(true);

			editArea.string = "IdentityDictionary[\n";
			dict.pairsDo({ |k, v|
				switch(k.class,
					Symbol, { key = "'"++k++"'" },
					Char, { key = "$"++k }
				);
				editArea.string_(editArea.string++"    "++key+"->"+v++",\n");
			});
			editArea.string_(editArea.string++"];");

			flow.nextLine.shift(0, 5);
			[name.bounds.height+editArea.bounds.height+16, editArea];
		};

		eas = ();

		eas.keyCodes = makeEditArea.("KeyDownActions.keyCodes", keyCodes, 400);
		// "eas.keyCodes: %\n".postf(eas.keyCodes);
		scrollView.bounds = Rect(0, 0, scrollView.bounds.width, eas.keyCodes[0]);

		if(GUI.id !== \cocoa) {
			eas.modifiersQt = makeEditArea.("KeyDownActions.modifiersQt", modifiersQt, 100);
			scrollView.bounds = Rect(0, 0, scrollView.bounds.width, scrollView.bounds.height+eas.modifiersQt[0]);

			if(arrowsModifiersQt !== modifiersQt) {
				eas.arrowsModifiersQt = makeEditArea.("KeyDownActions.arrowsModifiersQt", arrowsModifiersQt, 100);
				scrollView.bounds = Rect(0, 0, scrollView.bounds.width, scrollView.bounds.height+eas.arrowsModifiersQt[0]);
			}
		} {
			eas.modifiersCocoa = makeEditArea.("KeyDownActions.modifiersCocoa", modifiersCocoa, 100);
			scrollView.bounds = Rect(0, 0, scrollView.bounds.width, scrollView.bounds.height+eas.modifiersCocoa[0]);

			if(arrowsModifiersCocoa !== modifiersCocoa) {
				eas.arrowsModifiersCocoa = makeEditArea.("KeyDownActions.arrowsModifiersCocoa", arrowsModifiersCocoa, 100);
				scrollView.bounds = Rect(0, 0, scrollView.bounds.width, scrollView.bounds.height+eas.arrowsModifiersCocoa[0]);
			}
		};

		parent ?? { window.front };
		all.add(this);
	}

	result { |write|
		var res = IdentityDictionary.new, tmp;
		var keyCodesPath, guiId;

		switch(GUI.id,
			\cocoa, { guiId = "Cocoa" },
			{ guiId = "Qt" },
		);

		keyCodesPath = this.class.filenameSymbol.asString.dirname +/+ "keyCodesAndMods"++guiId;

		if((tmp = eas.keyCodes[1].string.interpret).size > 0) { res.put(\keyCodes, tmp) };
		if(GUI.id !== \cocoa, {
			if((tmp = eas.modifiersQt[1].string.interpret).size > 0, { res.put(\modifiersQt, tmp) });
			eas.arrowsModifiersQt !? {
				if(eas.arrowsModifiersQt[1].string.interpret.size > 0 and:{
					tmp !== eas.arrowsModifiersQt[1].string.interpret
				}) {
					res.put(\arrowsModifiersQt, eas.arrowsModifiersQt[1].string.interpret)
				} { res.arrowsModifiersQt = res.modifiersQt }
			}
		}, {
			if((tmp = eas.modifiersCocoa[1].string.interpret).size > 0) { res.put(\modifiersCocoa, tmp) };
			eas.arrowsModifiersCocoa !? {
				if(eas.arrowsModifiersCocoa[1].string.interpret.size > 0 and:{
					tmp !== eas.arrowsModifiersCocoa[1].string.interpret
				}) {
					res.put(\arrowsModifiersCocoa, eas.arrowsModifiersCocoa[1].string.interpret)
				} { res.arrowsModifiersCocoa = res.modifiersCocoa }
			}
		});
		if(write) { ^res.writeArchive(keyCodesPath) } { ^res }
	}

}