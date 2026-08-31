package com.qbank.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qbank.common.BusinessException;
import com.qbank.entity.Theme;
import com.qbank.mapper.ThemeMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 主题服务测试: 全局默认主题解析 / 默认移交 / 校验 / 首个主题自动默认
 */
class ThemeServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private Theme theme(Long id, String key, Integer enabled, Integer isDefault) {
        Theme t = new Theme();
        t.setId(id);
        t.setName(key);
        t.setThemeKey(key);
        t.setConfig("{\"primary\":\"#409eff\"}");
        t.setEnabled(enabled);
        t.setIsDefault(isDefault);
        return t;
    }

    private ThemeService service(ThemeMapper mapper) {
        return new ThemeService(mapper, objectMapper);
    }

    @Test
    void activePrefersDefault() {
        ThemeMapper mapper = mock(ThemeMapper.class);
        when(mapper.findDefault()).thenReturn(theme(1L, "dark", 1, 1));
        ThemeService service = service(mapper);
        assertThat(service.active().getThemeKey()).isEqualTo("dark");
        verify(mapper, never()).selectEnabled();
    }

    @Test
    void activeFallsBackToFirstEnabled() {
        ThemeMapper mapper = mock(ThemeMapper.class);
        when(mapper.findDefault()).thenReturn(null);
        when(mapper.selectEnabled()).thenReturn(List.of(theme(2L, "green", 1, 0), theme(3L, "default", 1, 0)));
        ThemeService service = service(mapper);
        assertThat(service.active().getThemeKey()).isEqualTo("green");
    }

    @Test
    void activeNullWhenNoEnabled() {
        ThemeMapper mapper = mock(ThemeMapper.class);
        when(mapper.findDefault()).thenReturn(null);
        when(mapper.selectEnabled()).thenReturn(List.of());
        ThemeService service = service(mapper);
        assertThat(service.active()).isNull();
    }

    @Test
    void addFirstThemeAutoDefault() {
        ThemeMapper mapper = mock(ThemeMapper.class);
        when(mapper.findByKey("default")).thenReturn(null);
        when(mapper.count()).thenReturn(1);
        // MyBatis 实际执行时会把自增主键回写到实体, 这里用 thenAnswer 模拟
        org.mockito.Mockito.doAnswer(inv -> {
            ((Theme) inv.getArgument(0)).setId(1L);
            return 1;
        }).when(mapper).insert(org.mockito.ArgumentMatchers.any(Theme.class));
        when(mapper.findById(1L)).thenReturn(theme(1L, "default", 1, 1));
        ThemeService service = service(mapper);
        Theme input = new Theme();
        input.setName("默认蓝");
        input.setThemeKey("default");
        input.setConfig("{}");
        service.add(input);
        verify(mapper).clearDefault();
        verify(mapper).setDefault(1L);
    }

    @Test
    void addDuplicateKeyThrows() {
        ThemeMapper mapper = mock(ThemeMapper.class);
        when(mapper.findByKey("default")).thenReturn(theme(1L, "default", 1, 1));
        ThemeService service = service(mapper);
        Theme input = new Theme();
        input.setName("重复");
        input.setThemeKey("default");
        assertThatThrownBy(() -> service.add(input))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("主题标识已存在");
        verify(mapper, never()).insert(input);
    }

    @Test
    void addInvalidKeyThrows() {
        ThemeService service = service(mock(ThemeMapper.class));
        Theme input = new Theme();
        input.setName("非法标识");
        input.setThemeKey("9start"); // 数字开头不合法
        assertThatThrownBy(() -> service.add(input))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("主题标识");
    }

    @Test
    void addInvalidConfigThrows() {
        ThemeMapper mapper = mock(ThemeMapper.class);
        when(mapper.findByKey("x")).thenReturn(null);
        ThemeService service = service(mapper);
        Theme input = new Theme();
        input.setName("坏配置");
        input.setThemeKey("x");
        input.setConfig("{not-json");
        assertThatThrownBy(() -> service.add(input))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("JSON");
        verify(mapper, never()).insert(input);
    }

    @Test
    void updateKeepsKeyAndPromotesWhenDefaultDisabled() {
        ThemeMapper mapper = mock(ThemeMapper.class);
        when(mapper.findById(1L)).thenReturn(theme(1L, "default", 1, 1));
        Theme disabled = theme(1L, "default", 0, 1);
        when(mapper.findById(1L)).thenReturn(theme(1L, "default", 1, 1), disabled);
        when(mapper.selectEnabled()).thenReturn(List.of(theme(2L, "dark", 1, 0)));
        ThemeService service = service(mapper);
        Theme input = new Theme();
        input.setId(1L);
        input.setName("默认蓝");
        input.setConfig("{}");
        input.setEnabled(0);
        service.update(input);
        // 标识保持原值
        assertThat(input.getThemeKey()).isEqualTo("default");
        // 默认已移交给 dark
        verify(mapper).clearDefault();
        verify(mapper).setDefault(2L);
    }

    @Test
    void updateMissingIdThrows() {
        ThemeService service = service(mock(ThemeMapper.class));
        Theme input = new Theme();
        input.setName("x"); // id 为 null
        assertThatThrownBy(() -> service.update(input))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("主题ID不能为空");
    }

    @Test
    void updateStatusDisableDefaultPromotes() {
        ThemeMapper mapper = mock(ThemeMapper.class);
        when(mapper.findById(1L)).thenReturn(theme(1L, "default", 1, 1));
        when(mapper.selectEnabled()).thenReturn(List.of(theme(2L, "dark", 1, 0)));
        ThemeService service = service(mapper);
        service.updateStatus(1L, 0);
        verify(mapper).updateStatus(1L, 0);
        verify(mapper).setDefault(2L);
    }

    @Test
    void updateStatusEnableOrdinaryNoPromote() {
        ThemeMapper mapper = mock(ThemeMapper.class);
        when(mapper.findById(2L)).thenReturn(theme(2L, "dark", 0, 0));
        ThemeService service = service(mapper);
        service.updateStatus(2L, 1);
        verify(mapper).updateStatus(2L, 1);
        verify(mapper, never()).clearDefault();
    }

    @Test
    void setDefaultClearsOthersAndEnables() {
        ThemeMapper mapper = mock(ThemeMapper.class);
        when(mapper.findById(2L)).thenReturn(theme(2L, "dark", 0, 0));
        ThemeService service = service(mapper);
        service.setDefault(2L);
        verify(mapper).clearDefault();
        verify(mapper).setDefault(2L);
        verify(mapper).updateStatus(2L, 1); // 设为默认自动启用
    }

    @Test
    void deleteDefaultPromotes() {
        ThemeMapper mapper = mock(ThemeMapper.class);
        when(mapper.findById(1L)).thenReturn(theme(1L, "default", 1, 1));
        when(mapper.selectEnabled()).thenReturn(List.of(theme(2L, "dark", 1, 0), theme(1L, "default", 1, 1)));
        ThemeService service = service(mapper);
        service.delete(1L);
        verify(mapper).setDefault(2L);
        verify(mapper).deleteById(1L);
    }

    @Test
    void deleteOrdinaryNoPromote() {
        ThemeMapper mapper = mock(ThemeMapper.class);
        when(mapper.findById(2L)).thenReturn(theme(2L, "dark", 1, 0));
        ThemeService service = service(mapper);
        service.delete(2L);
        verify(mapper, never()).clearDefault();
        verify(mapper).deleteById(2L);
    }

    @Test
    void deleteMissingThrows() {
        ThemeMapper mapper = mock(ThemeMapper.class);
        when(mapper.findById(9L)).thenReturn(null);
        ThemeService service = service(mapper);
        assertThatThrownBy(() -> service.delete(9L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("主题不存在");
    }
}