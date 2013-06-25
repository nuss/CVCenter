/* (c) 2010-2012 Stefan Nussbumer */
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

+ TabbedView {

	getLabels { ^labels }

	getLabelAt { |index|
		^labels[index].asString;
	}

	setLabelAt { |index, name|
		this.paintTab(tabViews[index], name.asString);
		labels[index] = name.asString;
		this.updateViewSizes();
	}

}

+ TabbedViewTab {

	// detachCVCTab { |parent|
	// 	var detachWin, flow;
	//
	// 	if(GUI.id == \qt) {
	// 		detachWin = Window("CVCenter", Rect(
	// 			widget.absoluteBounds.left+10,
	// 			widget.absoluteBounds.top+10,
	// 			parent.bounds.width+8,
	// 			parent.bounds.height+8
	// 		));
	// 		this.setParent(tabbedView.clone(detachWin));
	// 		{this.widget.mouseDown(4,4);this.widget.mouseMove(0,0);this.widget.mouseUp(0,0)}.defer(0.2);
	// 		^detachWin.front;
	// 	}
	// }

}