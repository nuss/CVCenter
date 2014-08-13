+CV {

	cvWidgetConnect { |view|
		^CV.viewDictionary[view.class].new(this, view);
	}

	cvWidgetDisconnect { |object|
		object.remove;
		// ^object = nil;
	}

	cvSplit {
		Routine { value.yield }
	}

}

+Array {

	cvWidgetConnect { |view|
		^CV.viewDictionary[view.class].new(this, view);
	}

	cvWidgetDisconnect { |object|
		object.remove;
		// ^object = nil;
	}

}