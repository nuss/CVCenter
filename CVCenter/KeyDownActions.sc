KeyDownActions {

	classvar <>actions;
	classvar <modifiers, <arrowsLinux, <arrowsOSX;

	*initClass {
		Class.initClassTree(Platform);
		modifiers = IdentityDictionary[
			// only use 'shift' or 'alt' modifiers
			\shift ->						131072,
			\alt ->							524288,
			'alt + shift' ->				655360,
		];

		arrowsOSX = IdentityDictionary[
			'arrow up' ->							(mod: 2097152, keycode: 126, key: 16777235),
			'arrow down' ->							(mod: 2097152, keycode: 125, key: 16777237),
			'arrow right' ->						(mod: 2097152, keycode: 124, key: 16777236),
			'arrow left' ->							(mod: 2097152, keycode: 123, key: 16777234),

			'alt + arrow up' ->						(mod: 2621440, keycode: 126, key: 16777235),
			'alt + arrow down' ->					(mod: 2621440, keycode: 125, key: 16777237),
			'alt + arrow right' ->					(mod: 2621440, keycode: 124, key: 16777236),
			'alt + arrow left' ->					(mod: 2621440, keycode: 123, key: 16777234),

			'alt + shift + arrow up' ->				(mod: 2752512, keycode: 126, key: 16777235),
			'alt + shift + arrow down' ->			(mod: 2752512, keycode: 125, key: 16777237),
			'alt + shift + arrow right' ->			(mod: 2752512, keycode: 124, key: 16777236),
			'alt + shift + arrow left' ->			(mod: 2752512, keycode: 123, key: 16777234),

			'shift + arrow up' ->				(mod: 2228224, keycode: 126, key: 16777235),
			'shift + arrow down' ->				(mod: 2228224, keycode: 125, key: 16777237),
			'shift + arrow right' ->			(mod: 2228224, keycode: 124, key: 16777236),
			'shift + arrow left' ->				(mod: 2228224, keycode: 123, key: 16777234)
		];

		arrowsLinux = IdentityDictionary[
			'arrow up' ->						(mod: 0, keycode: 111, key: 16777235),
			'arrow down' ->						(mod: 0, keycode: 116, key: 16777237),
			'arrow right' ->					(mod: 0, keycode: 114, key: 16777236),
			'arrow left' ->						(mod: 0, keycode: 113, key: 16777234),

			'alt + arrow up' ->					(mod: 524288, keycode: 111, key: 16777235),
			'alt + arrow down' ->				(mod: 524288, keycode: 116, key: 16777237),
			'alt + arrow right' ->				(mod: 524288, keycode: 114, key: 16777236),
			'alt + arrow left' ->				(mod: 524288, keycode: 113, key: 16777234),

			'alt + shift + arrow up' ->			(mod: 655360, keycode: 111, key: 16777235),
			'alt + shift + arrow down' ->		(mod: 655360, keycode: 116, key: 16777237),
			'alt + shift + arrow right' ->		(mod: 655360, keycode: 114, key: 16777236),
			'alt + shift + arrow left' ->		(mod: 655360, keycode: 113, key: 16777234),

			'shift + arrow up' ->				(mod: 131072, keycode: 111, key: 16777235),
			'shift + arrow down' ->				(mod: 131072, keycode: 116, key: 16777237),
			'shift + arrow right' ->			(mod: 131072, keycode: 114, key: 16777236),
			'shift + arrow left' ->				(mod: 131072, keycode: 113, key: 16777234)
		]
	}

}