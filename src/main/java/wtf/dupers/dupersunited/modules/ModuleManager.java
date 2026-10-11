package wtf.dupers.dupersunited.modules;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import wtf.dupers.dupersunited.api.module.Module;
import wtf.dupers.dupersunited.api.module.settings.Setting;
import wtf.dupers.dupersunited.commands.MainCommand;

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

    // there is probably, 1. a better way to do this, 2. a better place to put this
    public static void moduleLog(String module, String message) {
        moduleLog(Component.literal(module), Component.literal(message));
    }

    public static void moduleLog(Component module, String message) {
        moduleLog(module.copy(), Component.literal(message));
    }

    public static void moduleLog(String module, Component message) {
        moduleLog(Component.literal(module), message);
    }

    public static void moduleLog(MutableComponent moduleName, Component message) {
        MainCommand.sendMessage(Component.empty()
            .append(Component.literal("[").withStyle(ChatFormatting.GRAY))
            .append(moduleName.withStyle(ChatFormatting.DARK_RED))
            .append(Component.literal("]").withStyle(ChatFormatting.GRAY))
            .append(Component.literal(" "))
            .append(message), true);
    }
}