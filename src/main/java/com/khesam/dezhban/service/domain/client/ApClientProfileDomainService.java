package com.khesam.dezhban.service.domain.client;

import com.khesam.dezhban.dataaccess.local.entity.ApClientProfileEntity;
import com.khesam.dezhban.dataaccess.local.entity.ClientEntity;
import com.khesam.dezhban.dataaccess.local.repository.ApClientProfileRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ApClientProfileDomainService {

    private final ApClientProfileRepository apClientProfileRepository;

    public ApClientProfileDomainService(ApClientProfileRepository apClientProfileRepository) {
        this.apClientProfileRepository = apClientProfileRepository;
    }

    public Optional<ApClientProfileEntity> findByClientId(long clientId) {
        return apClientProfileRepository.findById(clientId);
    }

    public void replace(ClientEntity client, ProfileData data) {
        if (data == null) {
            apClientProfileRepository.deleteById(client.getId());
            return;
        }
        ApClientProfileEntity profile = apClientProfileRepository.findById(client.getId())
                .orElseGet(() -> {
                    ApClientProfileEntity value = new ApClientProfileEntity();
                    value.setClient(client);
                    return value;
                });
        profile.setApTitle(data.apTitle());
        profile.setApCode(data.apCode());
        profile.setApCallbackUrl(data.apCallbackUrl());
        apClientProfileRepository.save(profile);
    }

    public record ProfileData(String apTitle, String apCode, String apCallbackUrl) {
    }
}
