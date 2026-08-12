package com.khesam.dezhban.service.domain.user;

import com.khesam.dezhban.common.Gender;
import com.khesam.dezhban.common.Nationality;
import com.khesam.dezhban.dataaccess.local.entity.EndUserEntity;
import com.khesam.dezhban.dataaccess.local.entity.UserProfileEntity;
import com.khesam.dezhban.dataaccess.local.repository.UserProfileRepository;
import com.khesam.dezhban.service.domain.support.DomainException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserProfileDomainService {

    private final UserProfileRepository userProfileRepository;

    public UserProfileDomainService(UserProfileRepository userProfileRepository) {
        this.userProfileRepository = userProfileRepository;
    }

    public Optional<UserProfileEntity> findByUserId(long userId) {
        return userProfileRepository.findById(userId);
    }

    public UserProfileEntity requireByUserId(long userId) {
        return userProfileRepository.findById(userId)
                .orElseThrow(() -> DomainException.notFound("User profile not found"));
    }

    public UserProfileEntity create(EndUserEntity user, ProfileData data) {
        UserProfileEntity profile = new UserProfileEntity();
        profile.setEndUser(user);
        apply(profile, data);
        return userProfileRepository.save(profile);
    }

    public void replace(UserProfileEntity profile, ProfileData data) {
        apply(profile, data);
    }

    private void apply(UserProfileEntity profile, ProfileData data) {
        profile.setFirstName(data.firstName());
        profile.setLastName(data.lastName());
        profile.setGender(data.gender());
        profile.setNationalCode(data.nationalCode());
        profile.setIdNumber(data.idNumber());
        profile.setBirthDate(data.birthDate());
        profile.setFatherName(data.fatherName());
        profile.setLatinFirstName(data.latinFirstName());
        profile.setLatinLastName(data.latinLastName());
        profile.setLatinFatherName(data.latinFatherName());
        profile.setNationality(data.nationality());
        profile.setPostalCode(data.postalCode());
        profile.setProvince(data.province());
        profile.setCity(data.city());
        profile.setAddress(data.address());
        profile.setMobileNumber(data.mobileNumber());
        profile.setPhoneNumber(data.phoneNumber());
        profile.setEmail(data.email() == null ? null : data.email().toLowerCase());
    }

    public record ProfileData(
            String firstName,
            String lastName,
            Gender gender,
            String nationalCode,
            String idNumber,
            String birthDate,
            String fatherName,
            String latinFirstName,
            String latinLastName,
            String latinFatherName,
            Nationality nationality,
            String postalCode,
            String province,
            String city,
            String address,
            String mobileNumber,
            String phoneNumber,
            String email
    ) {
    }
}
