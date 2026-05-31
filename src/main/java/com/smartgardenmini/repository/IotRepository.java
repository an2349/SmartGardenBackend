package com.smartgardenmini.repository;

import com.smartgardenmini.model.Iot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IotRepository extends JpaRepository<Iot, Long> {
    List<Iot> findIotByUsername(String username);
    Optional<Iot> findById(Long id);
    Optional<Iot> findBymacId(String macId);
    boolean existsBymacId(String mac);
    void deleteBymacId(String macId);
}