CVWidgetSpecsEditor {
	var <window;

	*new { |wdgtName, controlsDict, environment|
		^super.new.init(wdgtName, controlsDict, environment)
	}
	
	init { |name, controls, environment|
		var cName, wdgtName, specEnterText, specSelect, enterTab;
		var cNameRect, cNameEnterTextRect, specEnterTextRect, specSelectRect, enterTabRect;
		var staticTextFont, staticTextColor, textFieldFont, textFieldFontColor, textFieldBg;
		var flow, lines;
		
		#cNameRect, cNameEnterTextRect, specEnterTextRect, specSelectRect, enterTabRect = Rect.new!5;
		[cNameRect, cNameEnterTextRect, specEnterTextRect, specSelectRect, enterTabRect].do({ |e|
			e.height_(20).left_(0).top_(0);
		});
		[cNameRect, cNameEnterTextRect, enterTabRect].do({ |e| e.width_(70) });
		[specSelectRect, specEnterTextRect].do({ |e| e.width_(200) });
		
		[cNameRect, cNameEnterTextRect, specEnterTextRect, specSelectRect, enterTabRect].do({ |r| r.bounds.postln });
		
		staticTextFont = Font(Font.defaultSansFace, 10);
		staticTextColor = Color(0.2, 0.2, 0.2);
		textFieldFont = Font(Font.defaultMonoFace, 9);
		textFieldFontColor = Color.black;
		textFieldBg = Color.white;
		
		if(name.isNil or:{ controls.isNil }, {
			Error("CVWidgetSpecsEditor is a utilty-class, only to be used in connection with cvcGui and the like").throw;
		});
		
		lines = controls.size;
		
		window = Window("Specs:"+name, Rect(
			Window.screenBounds.width-650/2, 
			Window.screenBounds.height-(lines * 25 + 35)/2, 
			650, 
			lines * 25 + 35
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
			.string_(" widget-name")
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

		controls.pairsDo({ |name, spec|
			
			flow.shift(0, 0);
			StaticText(window, cNameRect)
				.font_(staticTextFont)
				.background_(Color(1.0, 1.0, 1.0, 0.5))
				.string_(""+name.asString)
			;
			
			flow.shift(5, 0);
			cName = TextField(window, cNameEnterTextRect)
				.font_(textFieldFont)
				.string_(name.asString)
				.background_(textFieldBg)
			;
			
			flow.shift(5, 0);
			specEnterText = TextField(window, specEnterTextRect)
				.font_(textFieldFont)
				.background_(textFieldBg)
			;
			
			flow.shift(5, 0);
			specSelect = PopUpMenu(window, specSelectRect);
			
			flow.shift(5, 0);
			enterTab = TextField(window, enterTabRect)
				.font_(textFieldFont)
				.background_(textFieldBg)
			;
		});
		
		window.front;
	}
	
}