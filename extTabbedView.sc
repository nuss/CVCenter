+ TabbedView {
	
	getLabels { ^labels }
	
	getLabelAt { |index|
		^labels[index].asString;
	}
	
	setLabelAt { |index, name|
		this.paintTab(tabViews[index], name.asString);
		labels[index] = name.asString;
		this.updateViewSizes();
	} 
	
}