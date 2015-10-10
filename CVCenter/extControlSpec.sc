+ControlSpec {

	safeHasZeroCrossing {
		var thisMinSign, thisMaxSign;
		#thisMinSign, thisMaxSign = [minval, maxval].collect{ |val|
			if(val.isArray) { val.sign.mean } { val.sign }
		};
		^thisMinSign != thisMaxSign or:{ (thisMinSign == 0).and(thisMaxSign == 0) };
	}

	excludingZeroCrossing {
		if(minval != 0 and:{ maxval != 0 }) {
			^this.hasZeroCrossing
		}
		^false
	}

	// multi-dimensional specs

	size {
		var size = [
			minval.size,
			maxval.size,
			step.size,
			this.default.size
		].maxItem;

		^if(size > 1, { size }, { 1 });
	}

	split {
		var specsArr = nil!this.size;
		if(this.size > 1) {
			this.size.do { |i|
				specsArr[i] = this.class.new(
					minval.asArray[i%this.size],
					maxval.asArray[i%this.size],
					warp,
					step.asArray[i%this.size],
					this.default.asArray[i%this.size],
					this.units
				)
			};
			^specsArr;
		} { ^this }
	}
}