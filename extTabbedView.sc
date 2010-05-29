+ TabbedView {
	
	labelNames {
		^labels;
	}
	
	labelNames_ { |names|
		labels.size.do({ |i|
			labels[i] = names[i];
		});
		this.updateViewSizes();
	}
	
	labelAt { |index|
		^labels[index];
	}
	
	labelAt_ { |name, index|
		labels[index] = name;
		this.updateViewSizes();
	} 
	
}