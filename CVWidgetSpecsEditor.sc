CVWidgetSpecsEditor {
	var <window;

	*new { |object, wdgtName, controlsDict, pairs2D, environment|
		^super.new.init(object, wdgtName, controlsDict, pairs2D, environment)
	}
	
	init { |obj, name, controls, pairs2D, environment|
		var object;
//		var cName, specEnterText, specSelect, enterTab;
		var wdgtName;
		var specsList, specsListSpecs, selectMatch;
		var cNameRect, cNameEnterTextRect, specEnterTextRect, specSelectRect, enterTabRect;
		var staticTextFont, staticTextColor, textFieldFont, selectFont, textFieldFontColor, textFieldBg;
		var formEls, nameStr, makeLine, sendBut, cancelBut;
		var flow, lines, allEls, allWidth;
		var cMatrix, specName, made;
				
		object = obj;
		
//		#cName, specEnterText, specSelect, enterTab = ()!4;

		#cNameRect, cNameEnterTextRect, specEnterTextRect, specSelectRect, enterTabRect = Rect.new!5;
		[cNameRect, cNameEnterTextRect, specEnterTextRect, specSelectRect, enterTabRect].do({ |e|
			e.height_(20).left_(0).top_(0);
		});
		[cNameRect, cNameEnterTextRect, enterTabRect].do({ |e| e.width_(70) });

		specSelectRect.width_(240);
		specEnterTextRect.width_(160);

		allEls = [cNameRect, cNameEnterTextRect, specEnterTextRect, specSelectRect];
				
		staticTextFont = Font(Font.defaultSansFace, 10);
		staticTextColor = Color(0.2, 0.2, 0.2);
		textFieldFont = Font(Font.defaultMonoFace, 10);
		selectFont = Font(Font.defaultMonoFace, 10, true);
		textFieldFontColor = Color.black;
		textFieldBg = Color.white;
		
		if(name.isNil or:{ controls.isNil }, {
			Error("CVWidgetSpecsEditor is a utilty-class, only to be used in connection with cvcGui and the like").throw;
		});
		
		lines = controls.asArray.flatten.size;
		
		window = Window("Specs:"+name, Rect(
			Window.screenBounds.width-650/2, 
			Window.screenBounds.height-(lines * 25 + 70)/2, 
			650, 
			lines * 25 + 70
		), scroll: true).userCanClose_(false);
		
		window.view.decorator = flow = FlowLayout(window.bounds.insetBy(5));
		flow.margin_(4@0);
		flow.gap_(0@4);
		
		flow.shift(0, 0);
		StaticText(window, cNameRect)
			.font_(staticTextFont)
			.string_(" argname")
		;

		flow.shift(5, 0);
		StaticText(window, cNameEnterTextRect)
			.font_(staticTextFont)
			.string_(" widget/key/spec")
		;

		flow.shift(5, 0);
		StaticText(window, specEnterTextRect)
			.font_(staticTextFont)
			.string_(" enter a ControlSpec")
		;

		flow.shift(5, 0);
		StaticText(window, specSelectRect)
			.font_(staticTextFont)
			.string_(" or select an existing one")
		;
		
		flow.shift(5, 0);
		StaticText(window, enterTabRect)
			.font_(staticTextFont)
			.string_(" tab-name")
		;
		
		specsList = ["Select a spec..."];		
		specsListSpecs = [nil];
		Spec.specs.pairsDo({ |k, v|
			if(v.isKindOf(ControlSpec), {
				specsList = specsList.add(k++":"+v);
				specsListSpecs = specsListSpecs.add(v);
			})
		});
				
		#formEls, cMatrix = ()!2;

		makeLine = { |elem, cname, size|
			
			if(elem.type.notNil, {
				"type: %\n".postf(elem.type);
				switch(elem.type,
					\w2d, {
						nameStr = ""+cname+"(lo/hi)";
						specName = cname;
					}, 
					\w2dcust, {
						nameStr = ""+cname;
						specName = cname.split($/);
						specName = specName[0]++(specName[1].firstToUpper);
					},
					{
						nameStr = ""+cname+"("++size++")";
						specName = cname;
					}
				)
			}, {
				nameStr = ""+cname;
				specName = cname;	
			});
			
			flow.shift(0, 0);
			StaticText(window, cNameRect)
				.font_(staticTextFont)
				.background_(Color(1.0, 1.0, 1.0, 0.5))
				.string_(nameStr)
			;
		
			flow.shift(5, 0);
			elem.cName = TextField(window, cNameEnterTextRect)
				.font_(textFieldFont)
				.string_(specName)
				.background_(textFieldBg)
			;
		
			flow.shift(5, 0);
			elem.specEnterText = TextField(window, specEnterTextRect)
				.font_(textFieldFont)
				.background_(textFieldBg)
			;
		
			flow.shift(5, 0);
			elem.specSelect = PopUpMenu(window, specSelectRect)
				.items_(specsList)
				.font_(selectFont)
			;
			
			selectMatch = specsListSpecs.detectIndex({ |ispec, i| ispec == cname.asSymbol.asSpec });
			selectMatch !? {
				elem.specSelect.value_(selectMatch);
			};
		
			flow.shift(5, 0);
			elem.enterTab = TextField(window, enterTabRect)
				.font_(textFieldFont)
				.background_(textFieldBg)
				.string_(name)
			;
		};
		
		made = [];

		controls.pairsDo({ |cname, spec, i|

			if(spec.class === Array, {
				if(spec.size == 2, {
					formEls.put(cname, ());
					formEls[cname].type = \w2d;
					makeLine.(formEls[cname], cname);
					made = made.add(cname);
				}, {
					// ???
//					spec.do({ |c, i| 
//						formEls[cname].put(i, ());
//						makeLine.(formEls[cname][i], cname, i, controls[cname].asArray.size);
//					})
				})
			});

			pairs2D !? {
				pairs2D.pairsDo({ |k, pair| 
					if(pair.includes(cname) and:{ 
						spec.class !== Array and:{ 
							made.includes(cname).not
						}
					}, { 
						formEls.put(cname, ());
						formEls[cname].type = \w2dcust;
						makeLine.(formEls[cname], pair[0]++"/"++pair[1]);
						made = made.add(pair[0]);
						made = made.add(pair[1]);
						made.postln;
					}) 
				})
			};

			if(formEls[cname].isNil and:{ made.indexOfEqual(cname).isNil }, { 
				formEls.put(cname, ()); 
				makeLine.(formEls[cname], cname);
				made = made.add(cname);
			})
			
		});
		
//		cMatrix.postln;
		
		allWidth = allEls.collect({ |e| e.width }).sum + (allEls.size-1*5);
				
		flow.shift(20, 10);
		cancelBut = Button(window, Rect(0, 0, 65, 20))
			.states_([[ "cancel", Color(0.1076096448114, 0.30322184313276, 0.14551632171296), Color(0.98753162717248, 0.77148428061484, 0.11016622702571)]])
			.action_({ |cb|  window.close })
		;
		
		flow.shift(5, 0);
		sendBut = Button(window, Rect(0, 0, 65, 20))
			.states_([[ "gui", Color.white, Color.red ]])
			.action_({ |sb|
//				formEls.postln; 
				formEls.pairsDo({ |el, vals| 
//					vals.postln;
					switch(vals.type,
						"w2d", {
//						el.postln;
							vals.removeAt(\type);
							vals.do({ |v|
								CVCenter.finishGui(obj, v);
							})
						},
						{
						
						}
					)
				});
//				CVCenter.finishGui();
				window.close;
			})
		;
		
		window.front;
	}
	
}