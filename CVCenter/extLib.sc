+ControlSpec {

	excludingZeroCrossing {
		if(minval != 0 and:{ maxval != 0 }) {
			^this.hasZeroCrossing
		}
	}

}