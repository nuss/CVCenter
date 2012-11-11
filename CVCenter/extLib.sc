+ControlSpec {

	excludingZeroCrossing {
		if(minval != 0 and:{ maxval != 0 }) {
			^this.hasZeroCrossing
		}
		^false
	}

}

+Array {

	cvCenterBuildCVConnections { | connectFunc, disconnectFuncBuilder, node, cvcKeys |
		var parameters, cvLinks, k, wdgtIndex, action;
		"cvcKeys: %\n".postf(cvcKeys);
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

			"cv, expr: %, %\n".postf(cv, expr);
			expr = expr ? cv;
			// "cv, expr: %, %\n".postf(cv, expr);
			if(expr.isNumber.not, {
				// if(cv.isArray, { action = nil!(cv.size) });
				cv.asArray.do({ |c, i|
					"c: %\n".postf(c);
					if((k = CVCenter.all.findKeyForValue(c)).notNil and:{ node.notNil }, {
						if(cv.size > 1, {
							// "action: %\n".postf(action);
							// "key, cv, index of c: %, %, %\n".postf(k, c, CVCenter.all.detectIndex({ |cv| cv === c }));
							action = nil!(cv.size);
							cv.do({ |cvau, i|
								if(cvau === CVCenter.at(k), {
									action[i] = "cv.value";
								}, {
									action[i] = "CVCenter.at('"++CVCenter.all.findKeyForValue(cvau)++"').value";
								})
							});
							// "action: %\n".postf(action);
							cvLinks.add(CVCenter.addActionAt(k, \default, "{ |cv| "++node++".setn('"++label++"', ["++action.join(",")++"]) }"));
							action = nil;
						}, {
							cvLinks.add(CVCenter.addActionAt(k, \default, "{ |cv| "++node++".set('"++label++"', cv.value) }"));
						})
					}, {
						cvLinks.add(c.action_({ connectFunc.value(label, expr) }));
					})
				})
			});
			[label,expr.value]
		};

		if (cvLinks.size > 0) { disconnectFuncBuilder.value(cvLinks)};
		^parameters;
	}

	cvcConnectToNode { |server, nodeID, node, cvcKeys|
		^this.cvCenterBuildCVConnections(
			{ | label, expr|
				var val, group, addAction, msg;
				"label, expr: %, %\n".postf(label, expr);
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
				"msg: %\n".postf(msg);
				server.sendBundle(server.latency, msg);
			}, { | cvLinks|
				node ?? {
					OSCpathResponder(server.addr, ["/n_end", nodeID],
						{ arg time, resp, msg; cvLinks.do({ arg n; n.remove}); resp.remove;}
					).add;
				}
			}, node, cvcKeys
		).flatten(1);
	}

}

+Object {

	getObjectVarNames { |environment|
		var interpreterVars, varNames = [], envs = [];
		var pSpaces = [], proxySpace;

		interpreterVars = #[a,b,c,d,e,f,g,h,i,j,k,l,m,n,p,q,r,s,t,u,v,w,x,y,z];

		varNames = varNames ++ interpreterVars.select({ |n|
			thisProcess.interpreter.perform(n) === this;
		});
		if(currentEnvironment.class !== ProxySpace, {
			currentEnvironment.pairsDo({ |k, v|
				if(v === this, { varNames = varNames.add("~"++(k.asString)) });
			})
		});

		switch(this.class,
			Synth, {
				environment !? {
					envs = interpreterVars.select({ |n|
						thisProcess.interpreter.perform(n) === environment;
					});
					currentEnvironment.pairsDo({ |k, v|
						if(v === environment, { envs = envs.add("~"++(k.asString)) });
					});
					environment.pairsDo({ |k, v|
						if(v === this, {
							envs = envs.collect({ |ev| ev = ev++"['"++k++"']" });
						})
					})
				};
				varNames = varNames++envs;
			},
			NodeProxy, {
				// the NodeProxy passed in could be part of a ProxySpace
				if(varNames.size < 1, {
					pSpaces = pSpaces ++ interpreterVars.select({ |n|
						thisProcess.interpreter.perform(n).class === ProxySpace;
					});
					if(currentEnvironment.class !== ProxySpace, {
						currentEnvironment.pairsDo({ |k, v|
							if(v.class === ProxySpace, { pSpaces = pSpaces.add("~"++k) });
						})
					});
					pSpaces.do({ |p|
						if(p.class === Symbol, {
							proxySpace = thisProcess.interpreter.perform(p);
						});
						if(p.class === String, {
							proxySpace = p.interpret;
						});
						if(proxySpace.respondsTo(\envir), {
							proxySpace.envir.pairsDo({ |k, v|
								if(v === this, {
									varNames = varNames.add(p.asString++"['"++k++"']");
								})
							})
						})
					})
				})

			},
			Ndef, {
				varNames = varNames.add(this.asString);
			}
		);
		^varNames;
	}

}