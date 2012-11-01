+ControlSpec {

	excludingZeroCrossing {
		if(minval != 0 and:{ maxval != 0 }) {
			^this.hasZeroCrossing
		}
	}

}

+Array {

	cvCenterBuildCVConnections { | connectFunc, disconnectFuncBuilder, node |
		var parameters, cvLinks, k;
		parameters = this.copy.clump(2);
		cvLinks = Array(parameters.size);
		parameters = parameters.collect { | p |
			var label, cv, expr;
			#label, cv = p;
			if (cv.isKindOf(Function)) { cv = cv.value };
			#cv, expr = cv.asArray;
			expr = expr ? cv;
			if (expr.isNumber.not) {
				cv.asArray.do { | cv |
					if((k = CVCenter.all.findKeyForValue(cv)).notNil and:{ node.notNil }, {
						cvLinks.add(CVCenter.addActionAt(k, \default, "{ |cv| "++node++".set('"++label++"', cv.value) }"));
					}, {
						cvLinks.add(cv.action_({connectFunc.value(label, expr)}));
					})
				}
			};
			[label,expr.value]
		};

		if (cvLinks.size > 0) { disconnectFuncBuilder.value(cvLinks)};
		^parameters;
	}

	cvcConnectToNode { |server, nodeID, node|
		^this.cvCenterBuildCVConnections(
			{ | label, expr|
				var val, group, addAction, msg;
				if (label != 'group') {
					val = expr.value.asArray;
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
				server.sendBundle(server.latency, msg);
			}, { | cvLinks|
				node ?? {
					OSCpathResponder(server.addr, ["/n_end", nodeID],
						{ arg time, resp, msg; cvLinks.do({ arg n; n.remove}); resp.remove;}
					).add;
				}
			}, node
		).flatten(1);
	}


}