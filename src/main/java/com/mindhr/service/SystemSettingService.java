package com.mindhr.service;

import com.mindhr.dto.SystemSettingRequest;
import com.mindhr.dto.SystemSettingResponse;
import com.mindhr.entity.SystemSetting;
import com.mindhr.exception.DuplicateResourceException;
import com.mindhr.exception.ResourceNotFoundException;
import com.mindhr.mapper.SystemSettingMapper;
import com.mindhr.repository.SystemSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SystemSettingService {

    private final SystemSettingRepository systemSettingRepository;

    public List<SystemSettingResponse> listAll() {
        return systemSettingRepository.findAll().stream()
                .map(SystemSettingMapper::toResponse)
                .toList();
    }

    public SystemSettingResponse getById(Long id) {
        return SystemSettingMapper.toResponse(findEntity(id));
    }

    public SystemSettingResponse getByKey(String key) {
        SystemSetting setting = systemSettingRepository.findByKeyIgnoreCase(key)
                .orElseThrow(() -> new ResourceNotFoundException("System setting not found: " + key));
        return SystemSettingMapper.toResponse(setting);
    }

    @Transactional
    public SystemSettingResponse create(SystemSettingRequest request) {
        if (systemSettingRepository.existsByKeyIgnoreCase(request.key())) {
            throw new DuplicateResourceException("A system setting with key '" + request.key() + "' already exists");
        }

        SystemSetting setting = new SystemSetting();
        setting.setKey(request.key());
        setting.setValue(request.value());
        setting.setDescription(request.description());
        return SystemSettingMapper.toResponse(systemSettingRepository.save(setting));
    }

    @Transactional
    public SystemSettingResponse update(Long id, SystemSettingRequest request) {
        SystemSetting setting = findEntity(id);

        if (systemSettingRepository.existsByKeyIgnoreCaseAndIdNot(request.key(), id)) {
            throw new DuplicateResourceException("A system setting with key '" + request.key() + "' already exists");
        }

        setting.setKey(request.key());
        setting.setValue(request.value());
        setting.setDescription(request.description());
        return SystemSettingMapper.toResponse(setting);
    }

    @Transactional
    public void delete(Long id) {
        if (!systemSettingRepository.existsById(id)) {
            throw new ResourceNotFoundException("System setting not found: " + id);
        }
        systemSettingRepository.deleteById(id);
    }

    private SystemSetting findEntity(Long id) {
        return systemSettingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("System setting not found: " + id));
    }
}
