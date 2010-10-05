
+SCView {
	reflow { 
		var children, decorator;
		children = this.children;
		decorator = this.decorator;
		children.do({ |child, i|
			if(i==0, {
				decorator.left_(decorator.margin.x);
				decorator.top_(decorator.margin.y);
			});
			decorator.place(child);
		})
	}
}