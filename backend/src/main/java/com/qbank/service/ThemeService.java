package com.qbank.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qbank.common.BusinessException;
import com.qbank.entity.Theme;
import com.qbank.mapper.ThemeMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 前端样式主题服务: 管理员维护多套主题, 全局默认主题停用/删除时自动移交
 */
@Service
public class ThemeService {

    private final ThemeMapper themeMapper;
    private final ObjectMapper objectMapper;

    public ThemeService(ThemeMapper themeMapper, ObjectMapper objectMapper) {
        this.themeMapper = themeMapper;
        this.objectMapper = objectMapper;
    }

    /** 当前全局生效主题(无需登录): 默认主题优先, 否则取首个启用主题, 无则 null */
    public Theme active() {
        Theme theme = themeMapper.findDefault();
        if (theme == null) {
            List<Theme> enabled = themeMapper.selectEnabled();
            if (!enabled.isEmpty()) {
                theme = enabled.get(0);
            }
        }
        return theme;
    }

    public List<Theme> list() {
        return themeMapper.selectAll();
    }

    public List<Theme> enabled() {
        return themeMapper.selectEnabled();
    }

    public Theme add(Theme theme) {
        validate(theme, true);
        if (themeMapper.findByKey(theme.getThemeKey()) != null) {
            throw new BusinessException("主题标识已存在");
        }
        normalize(theme);
        themeMapper.insert(theme);
        // 首个主题自动设为全局默认
        if (themeMapper.count() == 1) {
            themeMapper.clearDefault();
            themeMapper.setDefault(theme.getId());
        }
        return requireTheme(theme.getId());
    }

    public Theme update(Theme theme) {
        validate(theme, false);
        Theme exist = requireTheme(theme.getId());
        theme.setThemeKey(exist.getThemeKey()); // 标识不可修改
        themeMapper.update(theme);
        Theme updated = requireTheme(theme.getId());
        // 停用当前默认主题时, 自动把默认移交其他启用主题, 保证始终有全局样式
        if (updated.getIsDefault() != null && updated.getIsDefault() == 1
                && (updated.getEnabled() == null || updated.getEnabled() != 1)) {
            promoteDefault(theme.getId());
        }
        return requireTheme(theme.getId());
    }

    public void updateStatus(Long id, Integer enabled) {
        Theme theme = requireTheme(id);
        int status = enabled != null && enabled == 1 ? 1 : 0;
        themeMapper.updateStatus(id, status);
        // 停用默认主题时移交默认
        if (status != 1 && theme.getIsDefault() != null && theme.getIsDefault() == 1) {
            promoteDefault(id);
        }
    }

    public void setDefault(Long id) {
        Theme theme = requireTheme(id);
        themeMapper.clearDefault();
        themeMapper.setDefault(id);
        // 设为默认即视为启用, 保证全局样式可用
        if (theme.getEnabled() == null || theme.getEnabled() != 1) {
            themeMapper.updateStatus(id, 1);
        }
    }

    public void delete(Long id) {
        Theme theme = requireTheme(id);
        if (theme.getIsDefault() != null && theme.getIsDefault() == 1) {
            promoteDefault(id);
        }
        themeMapper.deleteById(id);
    }

    /** 把默认主题移交给除 exceptId 外首个启用主题(全部停用时则无默认, 前端回落内置样式) */
    private void promoteDefault(Long exceptId) {
        Theme next = themeMapper.selectEnabled().stream()
                .filter(t -> !t.getId().equals(exceptId))
                .findFirst().orElse(null);
        if (next != null) {
            themeMapper.clearDefault();
            themeMapper.setDefault(next.getId());
        }
    }

    private Theme requireTheme(Long id) {
        if (id == null) {
            throw new BusinessException("主题ID不能为空");
        }
        Theme theme = themeMapper.findById(id);
        if (theme == null) {
            throw new BusinessException("主题不存在");
        }
        return theme;
    }

    private void validate(Theme theme, boolean forAdd) {
        if (!StringUtils.hasText(theme.getName()) || theme.getName().trim().length() > 20) {
            throw new BusinessException("主题名称不能为空且不超过20个字符");
        }
        if (forAdd && (!StringUtils.hasText(theme.getThemeKey())
                || !theme.getThemeKey().matches("[a-zA-Z][a-zA-Z0-9_-]{0,49}"))) {
            throw new BusinessException("主题标识须以字母开头, 由字母/数字/_/-组成(最多50位)");
        }
        if (StringUtils.hasText(theme.getConfig())) {
            try {
                objectMapper.readTree(theme.getConfig());
            } catch (Exception e) {
                throw new BusinessException("样式配置不是合法的JSON");
            }
        }
    }

    private void normalize(Theme theme) {
        theme.setName(theme.getName().trim());
        if (theme.getThemeKey() != null) {
            theme.setThemeKey(theme.getThemeKey().trim());
        }
        if (theme.getEnabled() == null) {
            theme.setEnabled(1);
        }
        if (theme.getIsDefault() == null) {
            theme.setIsDefault(0);
        }
        if (!StringUtils.hasText(theme.getConfig())) {
            theme.setConfig("{}");
        }
    }
}