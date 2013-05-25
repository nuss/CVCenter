CVCenterKeyDownActions {

	// classvar <allEditors;
	classvar <viewActions;
	classvar <keyCodes, <modifiers, <arrowsModifiers;
	var <window, <>actions;

	*initClass {
		Class.initClassTree(Platform);
		Class.initClassTree(GUI);

		viewActions = IdentityDictionary.new;

		Platform.case(
			\linux, {
				keyCodes = IdentityDictionary[
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

				modifiers = arrowsModifiers = IdentityDictionary[
					\none ->			0,
					\shift ->			131072,
					\alt ->				524288,
					'alt + shift' ->	655360,
				];
			},

			\osx, {
				keyCodes = IdentityDictionary[
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
					\esc -> 			53,
					'arrow up' -> 		126,
					'arrow down' -> 	125,
					'arrow left' -> 	123,
					'arrow right' -> 	124,
				];

				switch(GUI.id,
					\cocoa, {
						arrowsModifiers = IdentityDictionary[
							'none' ->			10486016,
							'alt' ->			11010336,
							'shift' ->			10617090,
							'alt + shift' ->	11141410
						];

						modifiers = IdentityDictionary[
							\none ->			0,
							\alt ->				524576,
							\shift ->			131330,
							'alt + shift' ->	655650,
						]
					},
					\qt, {
						arrowsModifiers = IdentityDictionary[
							\none ->			2097152,
							'alt' ->			2621440,
							'shift' ->			2228224,
							'alt + shift' ->	2752512
						];

						modifiers = IdentityDictionary[
							\none ->			0,
							\shift ->			131072,
							\alt ->				524288,
							'alt + shift' ->	655360,
						];
					}
				)
			},

			\windows, {
				keyCodes = IdentityDictionary[
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
					$` -> 				192,
					'arrow up' -> 		38,
					'arrow down' -> 	40,
					'arrow left' -> 	37,
					'arrow right' -> 	39,
				];

				modifiers = arrowsModifiers = IdentityDictionary[
					\none ->			0,
					\shift -> 			131072,
					\alt -> 			524288,
					'alt + shift' -> 	655360,
				]
			}
		)
	}

	*editor { |parent, bounds, shortcutsDict, save=true|
		^super.new.init(parent, bounds, shortcutsDict, save);
	}

	init { |parent, bounds, shortcutsDict, save|
		var thisName, viewName;
		var scrollArea, scrollView, editAreas, editButs, removeButs, butArea, newBut, saveBut;
		var makeEditArea, shortcutFields, funcFields;
		var scrollFlow, editFlows, butFlow;
		var editAreasBg, shortCutColor, shortCutFont, textFieldFont;
		var order, orderedShortcuts;
		var tmpEditFlow;

		editAreasBg = Color(0.8, 0.8, 0.8);
		shortCutColor = Color(0.1, 0.1, 0.1);
		shortCutFont = Font("Arial", 14, true);
		textFieldFont = Font("Andale Mono", 10);

		// if(name.isNil) { thisName = view.class } { thisName = name };

		if(parent.isNil) {
			window = Window("shortcut editor:"+thisName, bounds ?? { Rect(
				Window.screenBounds.width-600/2,
				Window.screenBounds.height-600/2,
				600, 600
			) });
			// view.onClose_({ window.close });
		} { window = parent };

		#editAreas, editFlows, editButs, removeButs = []!4;

		scrollArea = ScrollView(window.asView, Rect(
			0, 0, window.asView.bounds.width, window.asView.bounds.height-31
		)).hasHorizontalScroller_(false);
		butArea = CompositeView(parent.asView, Rect(
			0, window.asView.bounds.height-31, window.asView.bounds.width, 31
		));

		scrollView = CompositeView(scrollArea, Rect(
			0, 0, scrollArea.bounds.width, scrollArea.bounds.height
		));

		scrollView.decorator = scrollFlow = FlowLayout(scrollView.bounds, 0@0, 1@0);

		makeEditArea = { |shortcut, funcString|
			var count;
			editAreas = editAreas.add(
				CompositeView(scrollView, scrollFlow.bounds.width-20@100).background_(Color(0.8, 0.8, 0.8));
			);
			count = editAreas.size-1;
			scrollView.bounds_(Rect(
				scrollView.bounds.left,
				scrollView.bounds.top,
				scrollView.bounds.width,
				count * 100
			));
			editAreas[count].decorator = tmpEditFlow = FlowLayout(editAreas[count].bounds, 7@7, 2@2);
			shortcutFields = shortcutFields.add(
				StaticText(editAreas[count], tmpEditFlow.indentedRemaining.width-126@15);
			);
			shortcut !? {
				shortcutFields[count].string_(shortcut);
			};
			editButs = editButs.add(
				Button(editAreas[count], 60@15)
					.states_([["edit", shortCutColor]])
					.action_({ |bt|
						editAreas.do(_.background_(editAreasBg));
						editAreas[count].background_(Color.red);
					})
				;
			);
			removeButs = removeButs.add(
				Button(editAreas[count], 60@15)
					.states_([["remove", shortCutColor]])
					.action_({ |bt|

					})
				;
			);
			tmpEditFlow.nextLine;
			funcFields = funcFields.add(
				TextView(
					editAreas[count],
					tmpEditFlow.indentedRemaining.width@tmpEditFlow.indentedRemaining.height
				).string_(funcString).font_(textFieldFont);
			);
			// "editArea.bounds: %\nshortcutFields.bounds: %\neditBut.bounds: %\nremoveBut: %\nfuncField.bounds: %\n".postf(editAreas[count].bounds, shortcutFields[count].bounds, editButs[count].bounds, editButs[count].bounds, removeButs[count].bounds, funcFields[count].bounds);
		};

		// viewActions[view] !? {
		// 	viewActions[view].pairsDo({ |shortcut, funcString|
		// 		makeEditArea.(shortcut, funcString);
		// 	})
		// };
		order = shortcutsDict.order;

		order.do({ |shortcut, i|
			[shortcut, shortcutsDict[shortcut][\func]].postln;
			makeEditArea.(shortcut, shortcutsDict[shortcut][\func].replace("\t", "    "))
		});

		"editAreas.size: %\n".postf(editAreas.size);

		parent ?? { window.front };
	}

}