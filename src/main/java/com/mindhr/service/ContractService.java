package com.mindhr.service;

import com.mindhr.dto.ContractHistoryResponse;
import com.mindhr.dto.ContractRequest;
import com.mindhr.dto.ContractResponse;
import com.mindhr.dto.ContractStatusChangeRequest;
import com.mindhr.entity.Contract;
import com.mindhr.entity.ContractHistory;
import com.mindhr.entity.Organization;
import com.mindhr.entity.enums.ContractStatus;
import com.mindhr.exception.ResourceNotFoundException;
import com.mindhr.mapper.ContractHistoryMapper;
import com.mindhr.mapper.ContractMapper;
import com.mindhr.repository.ContractHistoryRepository;
import com.mindhr.repository.ContractRepository;
import com.mindhr.repository.OrganizationRepository;
import com.mindhr.repository.UserRepository;
import com.mindhr.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/** Status changes go through {@link #changeStatus} so every transition is recorded as a {@link ContractHistory} row. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContractService {

    private final ContractRepository contractRepository;
    private final ContractHistoryRepository contractHistoryRepository;
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;

    public List<ContractResponse> list(Long requestedOrganizationId) {
        Long organizationId = TenantContext.resolveOrganizationId(requestedOrganizationId);
        return contractRepository.findByOrganizationId(organizationId).stream()
                .map(ContractMapper::toResponse)
                .toList();
    }

    public ContractResponse getById(Long id) {
        return ContractMapper.toResponse(findAccessibleEntity(id));
    }

    public List<ContractHistoryResponse> history(Long id) {
        findAccessibleEntity(id);
        return contractHistoryRepository.findByContractIdOrderByChangedAtDesc(id).stream()
                .map(ContractHistoryMapper::toResponse)
                .toList();
    }

    @Transactional
    public ContractResponse create(ContractRequest request) {
        Long organizationId = TenantContext.resolveOrganizationId(request.organizationId());
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found: " + organizationId));

        Contract contract = new Contract();
        contract.setOrganization(organization);
        contract.setStatus(ContractStatus.PENDING);
        applyRequest(contract, request);

        contract = contractRepository.save(contract);
        recordHistory(contract, null, contract.getStatus(), "Contract created");
        return ContractMapper.toResponse(contract);
    }

    @Transactional
    public ContractResponse update(Long id, ContractRequest request) {
        Contract contract = findAccessibleEntity(id);
        applyRequest(contract, request);
        return ContractMapper.toResponse(contract);
    }

    @Transactional
    public ContractResponse changeStatus(Long id, ContractStatusChangeRequest request) {
        Contract contract = findAccessibleEntity(id);
        ContractStatus fromStatus = contract.getStatus();
        contract.setStatus(request.status());
        recordHistory(contract, fromStatus, request.status(), request.note());
        return ContractMapper.toResponse(contract);
    }

    @Transactional
    public void delete(Long id) {
        Contract contract = findAccessibleEntity(id);
        contractRepository.delete(contract);
    }

    private void applyRequest(Contract contract, ContractRequest request) {
        contract.setTitle(request.title());
        contract.setCounterpartyName(request.counterpartyName());
        contract.setStartDate(request.startDate());
        contract.setEndDate(request.endDate());
        contract.setValue(request.value());
        contract.setCurrency(request.currency());
        contract.setTerms(request.terms());
    }

    private void recordHistory(Contract contract, ContractStatus fromStatus, ContractStatus toStatus, String note) {
        ContractHistory history = new ContractHistory();
        history.setContract(contract);
        history.setFromStatus(fromStatus);
        history.setToStatus(toStatus);
        history.setNote(note);
        history.setChangedAt(LocalDateTime.now());
        userRepository.findById(TenantContext.currentPrincipal().userId()).ifPresent(history::setChangedBy);
        contractHistoryRepository.save(history);
    }

    Contract findAccessibleEntity(Long id) {
        Contract contract = contractRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Contract not found: " + id));
        if (!TenantContext.canAccess(contract.getOrganization().getId())) {
            throw new ResourceNotFoundException("Contract not found: " + id);
        }
        return contract;
    }
}
