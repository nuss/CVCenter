+Object {
	
	// execute .changed for the given array of symbols
	changedKeys { |keys ... moreArgs|
		keys.do({ |key|
			this.changed(key.asSymbol, *moreArgs);
		})
	}
	
	
}