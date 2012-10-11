CVCenterPreferences {

	classvar <window;

	*makeWindow {
		var tabs, flow0, flow1, flow3;

		if(window.isNil or:{ window.isClosed }, {
			window = Window("CVCenter: preferences", Rect(
				Window.screenBounds.width/2-250,
				Window.screenBounds.height/2-175,
				500, 350
			)).front;

			tabs = TabbedView(window, Rect(0, 1, window.bounds.width, window.bounds.height), ["general", "GUI properties", "MIDI preferences"], scroll: true);
			tabs.view.resize_(5);
			tabs.tabCurve_(4);
			tabs.views[0].decorator = flow0 = FlowLayout(window.view.bounds, 7@7, 3@3);
			tabs.views[1].decorator = flow1 = FlowLayout(window.view.bounds, 7@7, 3@3);
			tabs.views[2].decorator = flow1 = FlowLayout(window.view.bounds, 7@7, 3@3);
		});
		window.front;
	}

}