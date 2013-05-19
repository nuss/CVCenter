KeyDownActions {

	classvar <>actions;
	classvar <keyCodesLinux, <keyCodesMac;
	classvar <modifiers, <arrowsLinux, <arrowsOSX;

	*initClass {
		Class.initClassTree(Platform);

		keyCodesLinux = IdentityDictionary[
			$1 ->				10,
			$2 ->				11,
			$3 ->				12,
			$4 ->				13,
			$5 ->				14,
			$6 ->				15,
			$7 ->				16,
			$8 ->				17,
			$9 ->				18,
			$0 ->				19,
			$- ->				20,
			$= ->				21,
			$q ->				24,
			$w ->				25,
			$e ->				26,
			$r ->				27,
			$t ->				28,
			$y ->				29,
			$u ->				30,
			$i ->				31,
			$o ->				32,
			$p ->				33,
			$[ ->				34,
			$] ->				35,
			$a ->				38,
			$s ->				39,
			$d ->				40,
			$f ->				41,
			$g ->				42,
			$h ->				43,
			$j ->				44,
			$k ->				45,
			$l ->				46,
			$; ->				47,
			$' ->				48,
			(92.asAscii) ->		51,
			$< ->				94,
			$z ->				52,
			$x ->				53,
			$c ->				54,
			$v ->				55,
			$b ->				56,
			$n ->				57,
			$m ->				58,
			$, ->				59,
			$. ->				60,
			$/ ->				61,
			\esc ->				9,
			$` ->				49,
			'arrow up' ->		111,
			'arrow down' ->		116,
			'arrow left' ->		113,
			'arrow right' ->	114,
		];

		modifiers = IdentityDictionary[
			// \cmd -> 						1048576,
			// 'cmd + alt' ->					1572864,
			// 'cmd + shift' ->				1179648,
			// 'cmd + ctrl + alt' ->			1835008,
			// 'cmd + ctrl' ->					1310720,
			// 'cmd + ctrl + shift' ->			1441792,
			// 'cmd + ctrl + alt + shift' ->	1966080,
			\shift ->						131072,
			\alt ->							524288,
			'alt + shift' ->				655360,
			// 'alt + ctrl' ->  				786432,
			// rather don't use under linux
			// \ctrl ->						262144,
			// 'ctrl + shift' ->				393216,
			// 'ctrl + alt + shift' ->			917504,
		];

		arrowsOSX = IdentityDictionary[
			'arrow up' ->							(mod: 2097152, keycode: 126, key: 16777235),
			'arrow down' ->							(mod: 2097152, keycode: 125, key: 16777237),
			'arrow right' ->						(mod: 2097152, keycode: 124, key: 16777236),
			'arrow left' ->							(mod: 2097152, keycode: 123, key: 16777234),

			'cmd + arrow up' ->						(mod: 3145728, keycode: 126, key: 16777235),
			'cmd + arrow down' ->					(mod: 3145728, keycode: 125, key: 16777237),
			'cmd + arrow right' ->					(mod: 3145728, keycode: 124, key: 16777236),
			'cmd + arrow left' ->					(mod: 3145728, keycode: 123, key: 16777234),

			'cmd + alt + arrow up' ->				(mod: 3670016, keycode: 126, key: 16777235),
			'cmd + alt + arrow down' ->				(mod: 3670016, keycode: 125, key: 16777237),
			'cmd + alt + arrow right' ->			(mod: 3670016, keycode: 124, key: 16777236),
			'cmd + alt + arrow left' ->				(mod: 3670016, keycode: 123, key: 16777234),

			'cmd + alt + ctrl + arrow up' ->		(mod: 3932160, keycode: 126, key: 16777235),
			'cmd + alt + ctrl + arrow down' ->		(mod: 3932160, keycode: 125, key: 16777237),
			'cmd + alt + ctrl + arrow right' ->		(mod: 3932160, keycode: 124, key: 16777236),
			'cmd + alt + ctrl + arrow left' ->		(mod: 3932160, keycode: 123, key: 16777234),

			'cmd + alt + ctrl + shift + arrow up' ->	(mod: 4063232, keycode: 126, key: 16777235),
			'cmd + alt + ctrl + shift + arrow down' ->	(mod: 4063232, keycode: 125, key: 16777237),
			'cmd + alt + ctrl + shift + arrow right' ->	(mod: 4063232, keycode: 124, key: 16777236),
			'cmd + alt + ctrl + shift + arrow left' ->	(mod: 4063232, keycode: 123, key: 16777234),

			'cmd + ctrl + shift + arrow up' ->		(mod: 3538944, keycode: 126, key: 16777235),
			'cmd + ctrl + shift + arrow down' ->	(mod: 3538944, keycode: 125, key: 16777237),
			'cmd + ctrl + shift + arrow right' ->	(mod: 3538944, keycode: 124, key: 16777236),
			'cmd + ctrl + shift + arrow left' ->	(mod: 3538944, keycode: 123, key: 16777234),

			'cmd + ctrl + arrow up' ->				(mod: 3407872, keycode: 126, key: 16777235),
			'cmd + ctrl + arrow down' ->			(mod: 3407872, keycode: 125, key: 16777237),
			'cmd + ctrl + arrow right' ->			(mod: 3407872, keycode: 124, key: 16777236),
			'cmd + ctrl + arrow left' ->			(mod: 3407872, keycode: 123, key: 16777234),

			'alt + arrow up' ->						(mod: 2621440, keycode: 126, key: 16777235),
			'alt + arrow down' ->					(mod: 2621440, keycode: 125, key: 16777237),
			'alt + arrow right' ->					(mod: 2621440, keycode: 124, key: 16777236),
			'alt + arrow left' ->					(mod: 2621440, keycode: 123, key: 16777234),

			'alt + ctrl + arrow up' ->				(mod: 2883584, keycode: 126, key: 16777235),
			'alt + ctrl + arrow down' ->			(mod: 2883584, keycode: 125, key: 16777237),
			'alt + ctrl + arrow right' ->			(mod: 2883584, keycode: 124, key: 16777236),
			'alt + ctrl + arrow left' ->			(mod: 2883584, keycode: 123, key: 16777234),

			'alt + ctrl + shift + arrow up' ->		(mod: 3014656, keycode: 126, key: 16777235),
			'alt + ctrl + shift + arrow down' ->	(mod: 3014656, keycode: 125, key: 16777237),
			'alt + ctrl + shift + arrow right' ->	(mod: 3014656, keycode: 124, key: 16777236),
			'alt + ctrl + shift + arrow left' ->	(mod: 3014656, keycode: 123, key: 16777234),

			'alt + shift + arrow up' ->				(mod: 2752512, keycode: 126, key: 16777235),
			'alt + shift + arrow down' ->			(mod: 2752512, keycode: 125, key: 16777237),
			'alt + shift + arrow right' ->			(mod: 2752512, keycode: 124, key: 16777236),
			'alt + shift + arrow left' ->			(mod: 2752512, keycode: 123, key: 16777234),

			'ctrl + arrow up' ->				(mod: 2359296, keycode: 126, key: 16777235),
			'ctrl + arrow down' ->				(mod: 2359296, keycode: 125, key: 16777237),
			'ctrl + arrow right' ->				(mod: 2359296, keycode: 124, key: 16777236),
			'ctrl + arrow left' ->				(mod: 2359296, keycode: 123, key: 16777234),

			'ctrl + shift + arrow up' ->		(mod: 2490368, keycode: 126, key: 16777235),
			'ctrl + shift + arrow down' ->		(mod: 2490368, keycode: 125, key: 16777237),
			'ctrl + shift + arrow right' ->		(mod: 2490368, keycode: 124, key: 16777236),
			'ctrl + shift + arrow left' ->		(mod: 2490368, keycode: 123, key: 16777234),

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

			// 'alt + ctrl + arrow up' ->			(mod: 786432, keycode: 111, key: 16777235),
			// 'alt + ctrl + arrow down' ->		(mod: 786432, keycode: 116, key: 16777237),
			// 'alt + ctrl + arrow right' ->		(mod: 786432, keycode: 114, key: 16777236),
			// 'alt + ctrl + arrow left' ->		(mod: 786432, keycode: 113, key: 16777234),

			// 'alt + ctrl + shift + arrow up' ->		(mod: 917504, keycode: 111, key: 16777235),
			// 'alt + ctrl + shift + arrow down' ->	(mod: 917504, keycode: 116, key: 16777237),
			// 'alt + ctrl + shift + arrow right' ->	(mod: 917504, keycode: 114, key: 16777236),
			// 'alt + ctrl + shift + arrow left' ->	(mod: 917504, keycode: 113, key: 16777234),

			'alt + shift + arrow up' ->			(mod: 655360, keycode: 111, key: 16777235),
			'alt + shift + arrow down' ->		(mod: 655360, keycode: 116, key: 16777237),
			'alt + shift + arrow right' ->		(mod: 655360, keycode: 114, key: 16777236),
			'alt + shift + arrow left' ->		(mod: 655360, keycode: 113, key: 16777234),

			// 'ctrl + arrow up' ->				(mod: 262144, keycode: 111, key: 16777235),
			// 'ctrl + arrow down' ->				(mod: 262144, keycode: 116, key: 16777237),
			// 'ctrl + arrow right' ->				(mod: 262144, keycode: 114, key: 16777236),
			// 'ctrl + arrow left' ->				(mod: 262144, keycode: 113, key: 16777234),

			// 'ctrl + shift + arrow up' ->		(mod: 393216, keycode: 111, key: 16777235),
			// 'ctrl + shift + arrow down' ->		(mod: 393216, keycode: 116, key: 16777237),
			// 'ctrl + shift + arrow right' ->		(mod: 393216, keycode: 114, key: 16777236),
			// 'ctrl + shift + arrow left' ->		(mod: 393216, keycode: 113, key: 16777234),

			'shift + arrow up' ->				(mod: 131072, keycode: 111, key: 16777235),
			'shift + arrow down' ->				(mod: 131072, keycode: 116, key: 16777237),
			'shift + arrow right' ->			(mod: 131072, keycode: 114, key: 16777236),
			'shift + arrow left' ->				(mod: 131072, keycode: 113, key: 16777234)
		]
	}

}