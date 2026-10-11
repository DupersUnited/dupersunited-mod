package wtf.dupers.dupersunited.modules;

import wtf.dupers.dupersunited.api.module.Module;
import wtf.dupers.dupersunited.api.module.settings.Setting;

import java.util.*;

public record ModuleManager(List<Module> modules) {
    public ModuleManager(List<Module> modules) {
        this.modules = Collections.unmodifiableList(modules);
    }

    public <T extends Module> T getModule(Class<T> moduleClass) {
        return modules.stream()
            .filter(moduleClass::isInstance)
            .map(moduleClass::cast)
            .findFirst().orElse(null);
    }

    public Module getModuleByName(String name) {
        return modules.stream()
            .filter(m -> m.getName().equalsIgnoreCase(name) || m.getIdentifier().equalsIgnoreCase(name))
            .findFirst()
            .orElse(null);
    }

    public <T extends Module> boolean isEnabled(Class<T> moduleClass) {
        T m = getModule(moduleClass);
        return m != null && m.isEnabled();
    }

    public boolean isEnabled(String name) {
        Module m = getModuleByName(name);
        return m != null && m.isEnabled();
    }

    public List<Class<? extends Module>> getEnabledModules() {
        return modules.stream()
            .filter(Module::isEnabled)
            .map(Module::getClass)
            .collect(java.util.stream.Collectors.toList());
    }

    public Setting<?> getSetting(String moduleName, String settingName) {
        Module m = getModuleByName(moduleName);
        if (m == null) return null;
        return m.getSettingByName(settingName);
    }

    public void onTick() {
        for (Module module : modules) {
            if (module.isEnabled()) {
                module.onTick();
            }
        }
    }
}