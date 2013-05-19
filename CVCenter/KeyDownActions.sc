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
			\shift ->						131072,
			\alt ->							524288,
			'alt + shift' ->				655360,
		];

		arrowsOSX = IdentityDictionary[
			'arrow up' ->						(mod: 2097152, keycode: 126, key: 16777235),
			'arrow down' ->						(mod: 2097152, keycode: 125, key: 16777237),
			'arrow right' ->					(mod: 2097152, keycode: 124, key: 16777236),
			'arrow left' ->						(mod: 2097152, keycode: 123, key: 16777234),

			'alt + arrow up' ->					(mod: 2621440, keycode: 126, key: 16777235),
			'alt + arrow down' ->				(mod: 2621440, keycode: 125, key: 16777237),
			'alt + arrow right' ->				(mod: 2621440, keycode: 124, key: 16777236),
			'alt + arrow left' ->				(mod: 2621440, keycode: 123, key: 16777234),

			'alt + shift + arrow up' ->			(mod: 2752512, keycode: 126, key: 16777235),
			'alt + shift + arrow down' ->		(mod: 2752512, keycode: 125, key: 16777237),
			'alt + shift + arrow right' ->		(mod: 2752512, keycode: 124, key: 16777236),
			'alt + shift + arrow left' ->		(mod: 2752512, keycode: 123, key: 16777234),

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