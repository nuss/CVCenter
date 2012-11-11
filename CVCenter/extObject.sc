+Object {

	// execute .changed for the given array of symbols
	changedKeys { |keys ... moreArgs|
		keys.do({ |key|
			this.changed(key.asSymbol, *moreArgs);
		})
	}

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