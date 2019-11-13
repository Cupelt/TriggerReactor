/*******************************************************************************
 *     Copyright (C) 2019 wysohn
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
var itemStackType = Java.type('org.spongepowered.api.item.inventory.ItemStack')
validation = {
    "overloads": [
        [{"name": "item", "type": itemStackType.class}]
    ]
}
function helditemlore(args){
    var item = args[0];
	var ArrayList = Java.type('java.util.ArrayList');
    var lores = item.get(Keys.ITEM_LORE).orElse(new ArrayList());
    var loreString = "";
    for(var k = 0; k < lores.length; k++){
        lore = lores[k].toPlain();
        if(k == (lores.length - 1))
            loreString = loreString + lore;
        else
            loreString = loreString + lore + "\n";
    }
	return loreString;
}