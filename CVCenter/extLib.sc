+ControlSpec {

	excludingZeroCrossing {
		if(minval != 0 and:{ maxval != 0 }) {
			^this.hasZeroCrossing
		}
		^false
	}

}

+Array {

	cvCenterBuildCVConnections { | server, nodeID, node, cvcKeys, setActive |
		var parameters, cvLinks, k, wdgtIndex, cvValues;
		var label, cv, expr;
		// ("cvCenterBuildConnections: "++[node, cvcKeys, server, nodeID]).postln;
		parameters = this.copy.clump(2);
		cvLinks = Array(parameters.size);
		parameters = parameters.collect { | p |
			// "p: %\n".postf(p);
			#label, cv = p;
			// "label, cv: %, %\n".postf(label, cv);
			if(cv.class !== Array, {
				if(cv.isKindOf(Function), { cv = cv.value });
				#cv, expr = cv.asArray;
			}, {
				cv.do({ |c, i|
					c.isKindOf(Function).if({ cv[i] = cv[i].value });
				})
			});

			// "cv, expr: %, %\n".postf(cv, expr);
			expr = expr ? cv;
			// "cv, expr: %, %\n".postf(cv, expr);
			if(expr.isNumber.not, {
				// if(cv.isArray, { action = nil!(cv.size) });
				cv.asArray.do({ |c, i|
					// "c, i: %, %\n".postf(c, i);
					if((k = CVCenter.all.findKeyForValue(c)).notNil, {
						if(cv.size > 1, {
							cvValues = nil!(cv.size);
							cv.do({ |cvau, i|
								if(cvau === CVCenter.at(k), {
									cvValues[i] = "cv.value";
								}, {
									cvValues[i] = "CVCenter.at('"++CVCenter.all.findKeyForValue(cvau)++"').value";
								})
							})
						});
						if(node.notNil, {
							if(cv.size > 1, {
								// "k, k.class, action: %\n".postf(k, k.class, action);
								cvLinks.add(CVCenter.addActionAt(
									k, "default_"++node.asString,
									"{ |cv| "++node++".setn('"++label++"', ["++cvValues.join(", ")++"]) }",
									active: setActive
								));
								cvValues = nil;
							}, {
								cvLinks.add(CVCenter.addActionAt(
									k, "default_"++node.asString,
									"{ |cv| "++node++".set('"++label++"', cv.value) }",
									active: setActive
								))
							})
						}, {
							if(cv.size > 1, {
								// [k, c.value].postln;
								cvLinks.add(CVCenter.addActionAt(
									k, \default,
									"{ |cv| Server('"++server++"').sendBundle("++server.latency++", ['/n_setn', "++nodeID++", '"++label++"', "++cv.size++", "++cvValues.join(", ")++"]) }"
								))
							}, {
								// [k, c.value].postln;
								cvLinks.add(CVCenter.addActionAt(
									k, \default,
									"{ |cv| Server('"++server++"').sendBundle("++server.latency++", ['/n_setn', "++nodeID++", '"++label++"', 1, cv.value]) }"
								))
							})
						})
					})
				})
			})
			// [label,expr.value]
		};

		// if (cvLinks.size > 0, {
		// 	"ending".postln;
		// 	// disconnectFuncBuilder.value(cvLinks);
		// });
		^parameters.flatten(1);
	}

}