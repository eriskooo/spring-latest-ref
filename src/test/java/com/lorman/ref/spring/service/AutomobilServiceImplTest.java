package com.lorman.ref.spring.service;

import com.lorman.ref.spring.client.DummyClient;
import com.lorman.ref.spring.domain.Automobil;
import com.lorman.ref.spring.dto.AutomobilDTO;
import com.lorman.ref.spring.exception.NotFoundException;
import com.lorman.ref.spring.mapper.AutomobilMapper;
import com.lorman.ref.spring.repository.AutoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Optional;

/**
 * Unit testy pre AutoService — biznis logika správy automobilov.
 * <p>
 * Čo sa tu testuje:
 * - findAll: zoznam áut s/bez stránkovania
 * - findById: nájdenie auta podľa ID, vrátane neexistujúceho ID
 * - create: nové auto dostane ID z DB (prichádzajúce ID sa ignoruje)
 * - update: zmeny sa uložia, yearMade môže byť vynechané
 * - deleteById: auto sa vymaže; neexistujúce ID vyhodí chybu
 * <p>
 * Infraštruktúra: čistý Mockito, bez Spring contextu — rýchle testy.
 */
@ExtendWith(MockitoExtension.class)
class AutomobilServiceImplTest {

    @Mock
    private AutoRepository repository;
    @Mock
    private DummyClient dummyClient;
    @Mock
    private TransactionTemplate transactionTemplate;

    private AutoServiceImpl service;

    // Testovacia sada — realistické dáta, aby boli asserty ľahko čitateľné
    private Automobil toyota2018;
    private Automobil vwGolf2020;

    @BeforeEach
    void setUp() {
        AutomobilMapper mapper = Mappers.getMapper(AutomobilMapper.class);
        service = new AutoServiceImpl(repository, mapper, dummyClient, transactionTemplate);

        toyota2018 = new Automobil(1L, "Toyota", "Corolla", 2018);
        vwGolf2020 = new Automobil(2L, "VW", "Golf", 2020);
    }

    // ── pomocná metóda: nasimuluje transakčný wrapper (TransactionTemplate.execute) ──────────
    private void mockTransactionTemplate() {
        Mockito.when(transactionTemplate.execute(Mockito.any()))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    TransactionCallback<List<AutomobilDTO>> cb =
                            (TransactionCallback<List<AutomobilDTO>>) invocation.getArgument(0);
                    return cb.doInTransaction(Mockito.mock(TransactionStatus.class));
                });
    }

    // ══════════════════════════════════════════════════════════════════
    //  findAll — zoznam automobilov
    // ══════════════════════════════════════════════════════════════════

    @Test
    void findAll_vrati_spravnu_stranku_ked_su_zadane_page_a_size() {
        Mockito.when(dummyClient.callDummy()).thenReturn(Mono.empty());
        Mockito.when(repository.findAll(Mockito.any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(toyota2018, vwGolf2020), PageRequest.of(0, 10), 2L));
        mockTransactionTemplate();

        StepVerifier.create(service.findAll(0, 10))
                .expectNextMatches(a -> a.getId().equals(1L) && a.getBrand().equals("Toyota"))
                .expectNextMatches(a -> a.getId().equals(2L) && a.getBrand().equals("VW"))
                .verifyComplete();
    }

    @Test
    void findAll_vrati_vsetky_auta_ked_nie_je_zadane_strankovanie() {
        Mockito.when(dummyClient.callDummy()).thenReturn(Mono.empty());
        Mockito.when(repository.findAll()).thenReturn(List.of(toyota2018, vwGolf2020));
        mockTransactionTemplate();

        StepVerifier.create(service.findAll(null, null))
                .expectNextMatches(a -> a.getId().equals(1L) && a.getBrand().equals("Toyota"))
                .expectNextMatches(a -> a.getId().equals(2L) && a.getBrand().equals("VW"))
                .verifyComplete();
    }

    // ══════════════════════════════════════════════════════════════════
    //  findById — vyhľadanie podľa ID
    // ══════════════════════════════════════════════════════════════════

    @Test
    void findById_najde_auto_ked_existuje() {
        Mockito.when(repository.findById(1L)).thenReturn(Optional.of(toyota2018));

        StepVerifier.create(service.findById(1L))
                .expectNextMatches(a -> a.getId().equals(1L) && a.getModel().equals("Corolla"))
                .verifyComplete();
    }

    @Test
    void findById_vyhodi_NotFoundException_ked_auto_neexistuje() {
        Mockito.when(repository.findById(99L)).thenReturn(Optional.empty());

        StepVerifier.create(service.findById(99L))
                .expectErrorMatches(ex -> ex instanceof NotFoundException
                        && ex.getMessage().contains("99"))
                .verify();
    }

    // ══════════════════════════════════════════════════════════════════
    //  create — pridanie nového auta
    // ══════════════════════════════════════════════════════════════════

    @Test
    void create_ignoruje_prichádzajúce_ID_a_dostane_nove_z_DB() {
        // Klient posiela ID=999, ale to sa musí zahodiť — ID prideľuje DB
        AutomobilDTO novePoziadavka = new AutomobilDTO(999L, "Skoda", "Octavia", 2019);
        Automobil ulozeneDoDB = new Automobil(10L, "Skoda", "Octavia", 2019);

        // Repozitár smie dostať iba entitu s id == null
        Mockito.when(repository.save(Mockito.argThat(a -> a.getId() == null)))
                .thenReturn(ulozeneDoDB);

        StepVerifier.create(service.create(novePoziadavka))
                .expectNextMatches(a -> a.getId().equals(10L) && a.getBrand().equals("Skoda"))
                .verifyComplete();
    }

    @Test
    void create_vyhodi_chybu_ked_brand_chyba() {
        AutomobilDTO bezBrandu = new AutomobilDTO(null, null, "Octavia", 2019);

        // validate() háže synchronne — Mono.defer zabezpečí, že výnimka sa propaguje cez reaktívny stream
        StepVerifier.create(Mono.defer(() -> service.create(bezBrandu)))
                .expectErrorMatches(ex -> ex instanceof IllegalArgumentException
                        && ex.getMessage().contains("brand"))
                .verify();
    }

    // ══════════════════════════════════════════════════════════════════
    //  update — aktualizácia existujúceho auta
    // ══════════════════════════════════════════════════════════════════

    @Test
    void update_zmeni_rok_vyroby_a_zachova_id() {
        // Požiadavka na zmenu roku výroby z 2018 na 2021
        AutomobilDTO zmenaNaRok2021 = new AutomobilDTO(null, "Toyota", "Corolla", 2021);
        Automobil ulozenaSVerziou2021 = new Automobil(1L, "Toyota", "Corolla", 2021);

        Mockito.when(repository.findById(1L)).thenReturn(Optional.of(toyota2018));
        Mockito.when(repository.save(Mockito.any())).thenReturn(ulozenaSVerziou2021);

        StepVerifier.create(service.update(1L, zmenaNaRok2021))
                .expectNextMatches(a -> a.getId().equals(1L) && a.getYearMade().equals(2021))
                .verifyComplete();
    }

    @Test
    void update_vyhodi_NotFoundException_ked_auto_neexistuje() {
        AutomobilDTO aktualizacia = new AutomobilDTO(null, "Toyota", "Corolla", 2021);
        Mockito.when(repository.findById(99L)).thenReturn(Optional.empty());

        StepVerifier.create(service.update(99L, aktualizacia))
                .expectErrorMatches(ex -> ex instanceof NotFoundException
                        && ex.getMessage().contains("99"))
                .verify();
    }

    // ══════════════════════════════════════════════════════════════════
    //  deleteById — vymazanie auta
    // ══════════════════════════════════════════════════════════════════

    @Test
    void deleteById_uspesne_vymaze_existujuce_auto() {
        Mockito.when(repository.findById(1L)).thenReturn(Optional.of(toyota2018));
        Mockito.doNothing().when(repository).deleteById(1L);

        StepVerifier.create(service.deleteById(1L))
                .verifyComplete();
    }

    @Test
    void deleteById_vyhodi_NotFoundException_ked_auto_neexistuje() {
        Mockito.when(repository.findById(99L)).thenReturn(Optional.empty());

        StepVerifier.create(service.deleteById(99L))
                .expectErrorMatches(ex -> ex instanceof NotFoundException
                        && ex.getMessage().contains("99"))
                .verify();
    }
}
