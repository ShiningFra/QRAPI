package com.example.QRAPI.repository;

import org.springframework.data.cassandra.repository.CassandraRepository;
import java.util.UUID;
import com.example.QRAPI.model.*;
import java.util.Optional;

public interface QRHashRepository extends CassandraRepository<QRHash, UUID> {
Optional<QRHash> findByHash(String hash);
}
