+ControlSpec {

	excludingZeroCrossing {
		if(minval != 0 and:{ maxval != 0 }) {
			^this.hasZeroCrossing
		}
		^false
	}

}

+Array {

	cvCenterBuildCVConnections { | connectFunc, disconnectFuncBuilder, node, cvcKeys, server, nodeID |
		var parameters, cvLinks, k, wdgtIndex, cvValues;
		("cvCenterBuildConnections: "++[connectFunc, disconnectFuncBuilder, node, cvcKeys]).postcs;
		parameters = this.copy.clump(2);
		cvLinks = Array(parameters.size);
		parameters = parameters.collect { | p |
			var label, cv, expr;
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
					// "c: %\n".postf(c);
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
								cvLinks.add(CVCenter.addActionAt(k, \default, "{ |cv| "++node++".setn('"++label++"', ["++cvValues.join(", ")++"]) }"));
								cvValues = nil;
							}, {
								cvLinks.add(CVCenter.addActionAt(k, \default, "{ |cv| "++node++".set('"++label++"', cv.value) }"));
							})
						}, {
							if(cv.size > 1, {
								cvLinks.add(CVCenter.addActionAt(k, \default, "{ |cv| Server('"++server++"').sendBundle("++server.latency++", ['/n_setn', "++nodeID++", '"++label++"', "++cv.size++", "++cvValues.join(", ")++"]) }"));
							}, {
								cvLinks.add(CVCenter.addActionAt(k, \default, "{ |cv| Server('"++server++"').sendBundle("++server.latency++", ['/n_setn', "++nodeID++", '"++label++"', 1, cv.value]) }"));
							})
						})
					})
				})
			});
			[label,expr.value]
		};

		if (cvLinks.size > 0) { disconnectFuncBuilder.value(cvLinks)};
		^parameters;
	}

	cvcConnectToNode { |server, nodeID, node, cvcKeys|
		("cvcConnectToNode: "++[server, nodeID, node, cvcKeys]).postln;
		^this.cvCenterBuildCVConnections(
			{ | label, expr|
				var val, group, addAction, msg;
				// "label, expr: %, %\n".postf(label, expr);
				if (label != 'group') {
					val = expr.collect({ |v| v.value });
					// "val.value: %\n".postf(val.value);
					msg = ['/n_setn', nodeID, label, val.size] ++ val;
				} {
					val = expr.asArray;
					group = val[0].value;
					addAction = val[1].value ? 0;
					msg = switch (addAction,
						0, { ['/g_head', group, nodeID] },
						1, { ['/g_tail', group, nodeID] },
						2, { ['/n_before', nodeID, group ] },
						3, { ['/n_after', nodeID, group ] }
					);
				};
				"msg: %\n".postf(msg);
				// "server: %\n".postf(server.asCompileString);
				server.sendBundle(server.latency, msg);
			}, { | cvLinks|
				node ?? {
					cvLinks.postln;
					OSCpathResponder(server.addr, ["/n_end", nodeID],
						{ arg time, resp, msg; cvLinks.do({ arg n; n.remove}); resp.remove;}
					).add;
				}
			}, node, cvcKeys, server, nodeID
		).flatten(1);
	}

}