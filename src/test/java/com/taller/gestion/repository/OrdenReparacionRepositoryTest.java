package com.taller.gestion.repository;

import com.taller.gestion.model.Cliente;
import com.taller.gestion.model.LineaOrden;
import com.taller.gestion.model.OrdenReparacion;
import com.taller.gestion.model.Vehiculo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

// @DataJpaTest usa la BD H2 de test (ver src/test/resources/application.properties)
// para comprobar que las derived queries y los JOIN FETCH nuevos funcionan de verdad
// contra un motor JPA real, no solo contra mocks.
@DataJpaTest
class OrdenReparacionRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private OrdenReparacionRepository ordenRepository;

    private Vehiculo crearVehiculoConCliente() {
        Cliente cliente = new Cliente();
        cliente.setNombre("Cliente Test");
        entityManager.persist(cliente);

        Vehiculo vehiculo = new Vehiculo();
        vehiculo.setMatricula("1234ABC");
        vehiculo.setMarca("Seat");
        vehiculo.setModelo("Ibiza");
        vehiculo.setTipo("COCHE");
        vehiculo.setCliente(cliente);
        entityManager.persist(vehiculo);
        return vehiculo;
    }

    @Test
    void findByCodigoSeguimientoEncuentraLaOrdenPorSuCodigoUnico() {
        Vehiculo vehiculo = crearVehiculoConCliente();
        OrdenReparacion orden = new OrdenReparacion();
        orden.setVehiculo(vehiculo);
        orden.setDescripcionProblema("Avería de prueba");
        entityManager.persistAndFlush(orden);
        String codigo = orden.getCodigoSeguimiento();

        Optional<OrdenReparacion> encontrada = ordenRepository.findByCodigoSeguimiento(codigo);

        assertThat(encontrada).isPresent();
        assertThat(encontrada.get().getIdOrden()).isEqualTo(orden.getIdOrden());
    }

    @Test
    void findByCodigoSeguimientoDevuelveVacioSiNoExiste() {
        assertThat(ordenRepository.findByCodigoSeguimiento("no-existe")).isEmpty();
    }

    @Test
    void findAllConVehiculoYLineasNoDuplicaFilasPorLaColeccionDeLineas() {
        Vehiculo vehiculo = crearVehiculoConCliente();
        OrdenReparacion orden = new OrdenReparacion();
        orden.setVehiculo(vehiculo);
        orden.setDescripcionProblema("Avería con varias líneas");

        LineaOrden l1 = new LineaOrden();
        l1.setTipo("MANO_OBRA");
        l1.setDescripcion("Mano de obra");
        l1.setCantidad(BigDecimal.ONE);
        l1.setPrecioUnitario(new BigDecimal("30.00"));
        orden.addLinea(l1);

        LineaOrden l2 = new LineaOrden();
        l2.setTipo("PIEZA");
        l2.setDescripcion("Filtro");
        l2.setCantidad(BigDecimal.ONE);
        l2.setPrecioUnitario(new BigDecimal("15.00"));
        orden.addLinea(l2);

        entityManager.persistAndFlush(orden);
        entityManager.clear();

        List<OrdenReparacion> resultado = ordenRepository.findAllConVehiculoYLineas();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getLineas()).hasSize(2);
    }
}
