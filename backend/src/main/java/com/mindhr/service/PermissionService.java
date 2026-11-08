package com.mindhr.service;

import com.mindhr.dto.PermissionRequest;
import com.mindhr.dto.PermissionResponse;
import com.mindhr.entity.Permission;
import com.mindhr.exception.DuplicateResourceException;
import com.mindhr.exception.ResourceNotFoundException;
import com.mindhr.mapper.PermissionMapper;
import com.mindhr.repository.PermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PermissionService {

    private final PermissionRepository permissionRepository;

    public List<PermissionResponse> listAll() {
        return permissionRepository.findAll().stream()
                .map(PermissionMapper::toResponse)
                .toList();
    }

    public PermissionResponse getById(Long id) {
        return PermissionMapper.toResponse(findEntity(id));
    }

    @Transactional
    public PermissionResponse create(PermissionRequest request) {
        if (permissionRepository.existsByModuleIgnoreCaseAndAction(request.module(), request.action())) {
            throw new DuplicateResourceException(
                    "Permission already exists for module '" + request.module() + "' and action " + request.action());
        }

        Permission permission = new Permission();
        permission.setModule(request.module());
        permission.setAction(request.action());
        return PermissionMapper.toResponse(permissionRepository.save(permission));
    }

    @Transactional
    public PermissionResponse update(Long id, PermissionRequest request) {
        Permission permission = findEntity(id);

        if (permissionRepository.existsByModuleIgnoreCaseAndActionAndIdNot(request.module(), request.action(), id)) {
            throw new DuplicateResourceException(
                    "Permission already exists for module '" + request.module() + "' and action " + request.action());
        }

        permission.setModule(request.module());
        permission.setAction(request.action());
        return PermissionMapper.toResponse(permission);
    }

    @Transactional
    public void delete(Long id) {
        if (!permissionRepository.existsById(id)) {
            throw new ResourceNotFoundException("Permission not found: " + id);
        }
        permissionRepository.deleteById(id);
    }

    private Permission findEntity(Long id) {
        return permissionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Permission not found: " + id));
    }
}
