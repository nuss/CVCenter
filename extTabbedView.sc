+ TabbedView {
	
	getLabelAt { |index|
		^labels[index];
	}
	
	setLabelAt { |index, name|
		this.paintTab(tabViews[index], name.asString);
		labels[index] = name;
		this.updateViewSizes();
	} 
	
}