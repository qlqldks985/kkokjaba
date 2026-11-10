package com.jjh.kkokjaba.zone.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jjh.kkokjaba.zone.domain.Zone;

public interface ZoneRepository extends JpaRepository<Zone, Long> {

	List<Zone> findAllByOrderByDisplayOrderAsc();

	Optional<Zone> findByName(String name);
}
