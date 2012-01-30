/* (c) Stefan Nussbaumer */
/* 
 This program is free software; you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation; either version 3 of the License, or
 (at your option) any later version.
 
 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.
 
 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301  USA
*/

OSCCommands {

	classvar collectFunc, running=false, cmdList;
	
	*initClass {
		cmdList = ();
		collectFunc = { |msg, time, addr, recvPort|
			if(msg[0] != '/status.reply', {
				cmdList.put(msg[0], msg[1..].size);
			})
		}
	}
	
	*collect { |play=true|
		var displayList, cmdList = ();
		if(play, {
			if(running == false, {
				thisProcess.addOSCRecvFunc(collectFunc);
				CmdPeriod.add({ this.collect(false) });
				running = true;
			})
		}, {
			thisProcess.removeOSCRecvFunc(collectFunc);
			CmdPeriod.remove({ this.collect(false) });
			running = false;
		});
	}
	
	*saveCmdSet { |deviceName|
		var thisDeviceName, allDevices, cmdsPath;
		
		thisDeviceName = deviceName.asSymbol;
		cmdsPath = this.filenameSymbol.asString.split($/).drop(-1).join($/);
		if(File.exists(cmdsPath+/+"OSCCommands"), {
			allDevices = Object.readArchive(cmdsPath+/+"OSCCommands");
		}, {
			allDevices = ();	
		});
		
		allDevices.put(thisDeviceName, cmdList).writeArchive(cmdsPath+/+"OSCCommands");
		cmdList.clear;
	}
	
	*deviceCmds { |deviceName|
		var thisDeviceName, thisCmds, cmdsPath;
		
		deviceName !? { thisDeviceName = deviceName.asSymbol };
		cmdsPath = this.filenameSymbol.asString.split($/).drop(-1).join($/)+/+"OSCCommands";
		thisCmds = Object.readArchive(cmdsPath);
		
		if(deviceName.notNil, { ^thisCmds[thisDeviceName] }, { ^thisCmds });
	}
	
	*clearCmds { |deviceName|
		//tomorrow
	}
	
}