CVCenterShortcutsEditor {
	classvar <window;

	*dialog {
		if(window.isNil or:{ window.isClosed }) {
			window = Window("edit temporary shortcuts", Rect(
				Window.screenBounds.width/2-249,
				Window.screenBounds.height/2-193,
				498, 385
			)).front;
		}
	}

}