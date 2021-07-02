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
package io.github.wysohn.triggerreactor.core.manager;

import io.github.wysohn.triggerreactor.core.main.TriggerReactorCore;
import io.github.wysohn.triggerreactor.core.script.interpreter.Executor;
import io.github.wysohn.triggerreactor.core.script.validation.ValidationException;
import io.github.wysohn.triggerreactor.core.script.validation.ValidationResult;
import io.github.wysohn.triggerreactor.core.script.validation.Validator;
import io.github.wysohn.triggerreactor.tools.timings.Timings;

import javax.script.*;
import java.io.*;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.*;

public abstract class AbstractExecutorManager extends AbstractJavascriptBasedManager implements KeyValueManager<Executor> {
    protected Map<String, Executor> jsExecutors = new HashMap<>();

    public AbstractExecutorManager(TriggerReactorCore plugin) throws ScriptException {
        super(plugin);
    }

    /**
     * Loads all the Executor files and files under the folders. If Executors are inside the folder, the folder
     * name will be added infront of them. For example, an Executor named test is under folder named hi, then
     * its name will be hi:test; therefore, you should #hi:test to call this executor.
     *
     * @param file   the target file/folder
     * @param filter the filter for Executors. Usually you check if the file ends withd .js or is a folder.
     * @throws ScriptException
     * @throws IOException
     */
    protected void reloadExecutors(File file, FileFilter filter) throws ScriptException, IOException {
        reloadExecutors(new Stack<String>(), file, filter);
    }

    private void reloadExecutors(Stack<String> name, File file, FileFilter filter) throws ScriptException, IOException {
        if (file.isDirectory()) {
            name.push(file.getName());
            for (File f : file.listFiles(filter)) {
                reloadExecutors(name, f, filter);
            }
            name.pop();
        } else {
            StringBuilder builder = new StringBuilder();
            for (int i = name.size() - 1; i >= 0; i--) {
                builder.append(name.get(i) + ":");
            }
            String fileName = file.getName();
            fileName = fileName.substring(0, fileName.indexOf("."));
            builder.append(fileName);

            if (jsExecutors.containsKey(builder.toString())) {
                plugin.getLogger().warning(builder.toString() + " already registered! Duplicating executors?");
            } else {
                JSExecutor exec = new JSExecutor(fileName, IScriptEngineInitializer.getEngine(sem), file);
                jsExecutors.put(builder.toString(), exec);
            }
        }
    }

    /* (non-Javadoc)
     * @see KeyValueManager#get(java.lang.Object)
     */
    @Override
    public Executor get(Object key) {
        return jsExecutors.get(key);
    }

    /* (non-Javadoc)
     * @see KeyValueManager#containsKey(java.lang.Object)
     */
    @Override
    public boolean containsKey(Object key) {
        return jsExecutors.containsKey(key);
    }

    /* (non-Javadoc)
     * @see KeyValueManager#entrySet()
     */
    @Override
    public Set<Entry<String, Executor>> entrySet() {
        Set<Entry<String, Executor>> set = new HashSet<>();
        for (Entry<String, Executor> entry : jsExecutors.entrySet()) {
            set.add(new AbstractMap.SimpleEntry<String, Executor>(entry.getKey(), entry.getValue()));
        }
        return set;
    }

    /* (non-Javadoc)
     * @see KeyValueManager#getExecutorMap()
     */
    @Override
    public Map<String, Executor> getBackedMap() {
        return this.jsExecutors;
    }

    public static class JSExecutor extends Executor {
        private final String executorName;
        private final String sourceCode;

        private ScriptEngine engine = null;
        private CompiledScript compiled = null;
        private boolean firstRun = true;
        private Validator validator = null;

        public JSExecutor(String executorName, ScriptEngine engine, File file) throws ScriptException, IOException {
            this(executorName, engine, new FileInputStream(file));
        }

        public JSExecutor(String executorName, ScriptEngine engine, InputStream file) throws ScriptException, IOException {
            this.executorName = executorName;
            this.engine = engine;

            StringBuilder builder = new StringBuilder();
            InputStreamReader reader = new InputStreamReader(file);
            int read = -1;
            while ((read = reader.read()) != -1)
                builder.append((char) read);
            reader.close();
            sourceCode = builder.toString();

            synchronized (engine.getContext()){
                Compilable compiler = (Compilable) engine;
                compiled = compiler.compile(sourceCode);
            }
        }

        private void registerValidationInfo(ScriptContext context) {
            Map<String, Object> validation = (Map<String, Object>) context.getAttribute("validation");
            if (validation == null) {
                return;
            }
            this.validator = Validator.from(validation);
        }

        public ValidationResult validate(Object... args) {
            if (firstRun) {
                throw new RuntimeException("the executor must be run at least once before using validate");
            }
            return validator.validate(args);
        }

        @Override
        public Integer execute(Timings.Timing timing, boolean sync, Map<String, Object> variables, Object e,
                               Object... args) throws Exception {
            Timings.Timing time = timing.getTiming("Executors").getTiming(executorName);
            time.setDisplayName("#" + executorName);

            ScriptContext scriptContext;
            synchronized (scriptContext = engine.getContext()){
                final Bindings bindings = engine.getBindings(ScriptContext.ENGINE_SCOPE);

                bindings.put("event", e);
                for (Map.Entry<String, Object> entry : variables.entrySet()) {
                    String key = entry.getKey();
                    Object value = entry.getValue();
                    bindings.put(key, value);
                }

                try {
                    compiled.eval(scriptContext);
                } catch (ScriptException e2) {
                    e2.printStackTrace();
                }

                if (firstRun) {
                    registerValidationInfo(scriptContext);
                    firstRun = false;
                }

                if (validator != null) {
                    ValidationResult result = validator.validate(args);
                    int overload = result.getOverload();
                    if (overload == -1) {
                        throw new ValidationException(result.getError());
                    }
                    scriptContext.setAttribute("overload", overload, ScriptContext.ENGINE_SCOPE);
                }

                Object jsObject = scriptContext.getAttribute(executorName);
                if (jsObject == null)
                    throw new Exception(executorName + ".js does not have 'function " + executorName + "()'.");
            }

            Callable<Integer> call = () -> {
                Object argObj = args;
                Object result = null;

                synchronized (engine.getContext()){
                    try (Timings.Timing t = time.begin(true)) {
                        result = ((Invocable) engine).invokeFunction(executorName ,argObj);
                    }
                }

                if (result instanceof Integer)
                    return (Integer) result;

                return null;
            };

            if (TriggerReactorCore.getInstance().isServerThread()) {
                Integer result = null;

                try {
                    result = call.call();
                } catch (Exception e1) {
                    e1.printStackTrace();
                    throw new Exception("#" + executorName + " encountered error.", e1);
                }
                return result;
            } else {
                Future<Integer> future = runSyncTaskForFuture(call);
                if (future == null) {
                    //probably server is shutting down
                    if (!TriggerReactorCore.getInstance().isEnabled()) {
                        return call.call();
                    } else {
                        throw new Exception("#" + executorName + " couldn't be finished. The server returned null Future.");
                    }
                } else {
                    Integer result = null;
                    try {
                        result = future.get(5, TimeUnit.SECONDS);
                    } catch (InterruptedException | ExecutionException e1) {
                        throw new Exception("#" + executorName + " encountered error.", e1);
                    } catch (TimeoutException e1) {
                        throw new Exception("#" + executorName + " was stopped. It took longer than 5 seconds to process. Is the server lagging?", e1);
                    }
                    return result;
                }
            }
        }
    }

    private static final Set<String> DEPRECATED_EXECUTORS = new HashSet<>();

    static {
        DEPRECATED_EXECUTORS.add("MODIFYPLAYER");
    }
}