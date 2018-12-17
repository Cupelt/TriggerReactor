/*******************************************************************************
 *     Copyright (C) 2018 wysohn
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
package io.github.wysohn.triggerreactor.core.manager.trigger;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import io.github.wysohn.triggerreactor.core.bridge.ICommandSender;
import io.github.wysohn.triggerreactor.core.main.TriggerReactor;
import io.github.wysohn.triggerreactor.core.script.wrapper.SelfReference;
import io.github.wysohn.triggerreactor.tools.FileUtil;

public abstract class AbstractCommandTriggerManager extends AbstractTriggerManager {
    protected final Map<String, CommandTrigger> commandTriggerMap = new HashMap<>();

    @Override
    public void reload() {
        commandTriggerMap.clear();

        for(File file : folder.listFiles()){
            if(!isTriggerFile(file))
                continue;

            String triggerName = extractName(file);

            File triggerConfigFile = new File(folder, triggerName+".yml");

            Boolean sync = Boolean.FALSE;
            if(triggerConfigFile.isFile() && triggerConfigFile.exists()){
                try {
                    sync = getData(triggerConfigFile, "sync", Boolean.FALSE);
                } catch (IOException e) {
                    e.printStackTrace();
                    continue;
                }
            }

            String script = null;
            try{
                script = FileUtil.readFromFile(file);
            } catch (IOException e) {
                e.printStackTrace();
                continue;
            }

            CommandTrigger trigger = null;
            try {
                trigger = new CommandTrigger(triggerName, file, script);
            } catch (TriggerInitFailedException e) {
                e.printStackTrace();
                continue;
            }

            trigger.setSync(sync);
            commandTriggerMap.put(triggerName, trigger);
        }
    }

    @Override
    public void saveAll(){
        Set<String> failed = new HashSet<>();
        for(Entry<String, CommandTrigger> entry : commandTriggerMap.entrySet()){
            String triggerName = entry.getKey();
            CommandTrigger trigger = entry.getValue();

            String script = trigger.getScript();

            File file = getTriggerFile(folder, triggerName, true);
            try{
                FileUtil.writeToFile(file, script);
            }catch(Exception e){
                e.printStackTrace();
                plugin.getLogger().severe("Could not save command trigger for "+triggerName);
                failed.add(triggerName);
            }

            File triggerConfigFile = new File(folder, triggerName+".yml");
            if(!triggerConfigFile.exists()){
                try {
                    triggerConfigFile.createNewFile();
                    setData(triggerConfigFile, "sync", trigger.isSync());
                } catch (IOException e) {
                    e.printStackTrace();
                    plugin.getLogger().severe("Could not save command trigger for "+triggerName);
                    failed.add(triggerName);
                }
            }
        }

        for(String key : failed){
            commandTriggerMap.remove(key);
        }
    }

    public boolean hasCommandTrigger(String cmd) {
        return commandTriggerMap.containsKey(cmd);
    }

    public CommandTrigger getCommandTrigger(String cmd){
        return commandTriggerMap.get(cmd);
    }

    /**
     *
     * @param adding CommandSender to send error message on script error
     * @param cmd command to intercept
     * @param script script to be executed
     * @return true on success; false if cmd already binded.
     */
    public boolean addCommandTrigger(ICommandSender adding, String cmd, String script) {
        if(commandTriggerMap.containsKey(cmd))
            return false;

        File triggerFile = getTriggerFile(folder, cmd, true);
        CommandTrigger trigger = null;
        try {
            trigger = new CommandTrigger(cmd, triggerFile, script);
        } catch (TriggerInitFailedException e1) {
            plugin.handleException(adding, e1);
            return false;
        }

        commandTriggerMap.put(cmd, trigger);

        plugin.saveAsynchronously(this);
        return true;
    }

    /**
     *
     * @param cmd command to stop intercept
     * @return true on success; false if cmd does not exist.
     */
    public boolean removeCommandTrigger(String cmd) {
        if(!commandTriggerMap.containsKey(cmd))
            return false;

        deleteInfo(commandTriggerMap.remove(cmd));

        return true;
    }

    public CommandTrigger createTempCommandTrigger(String script) throws TriggerInitFailedException {
        return new CommandTrigger("temp", null, script);
    }

    public AbstractCommandTriggerManager(TriggerReactor plugin, SelfReference ref, File tirggerFolder) {
        super(plugin, ref, tirggerFolder);
    }

    public static class CommandTrigger extends Trigger {

        public CommandTrigger(String name, File file, String script) throws TriggerInitFailedException {
            super(name, file, script);

            init();
        }

        @Override
        public Trigger clone() {
            try {
                return new CommandTrigger(triggerName, file, getScript());
            } catch (TriggerInitFailedException e) {
                e.printStackTrace();
            }
            return null;
        }
    }
}