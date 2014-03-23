+CV {

	cvWidgetConnect { |view|
		^CV.viewDictionary[view.class].new(this, view);
	}

	cvWidgetDisconnect { |object|
		object.remove;
	}

}

+Array {

	cvWidgetConnect { |view|
		^CV.viewDictionary[view.class].new(this, view);
	}

	cvWidgetDisconnect { |object|
		object.remove;
	}

}