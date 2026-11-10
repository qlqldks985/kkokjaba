package com.jjh.kkokjaba.missingcase.service;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jjh.kkokjaba.missingcase.domain.CaseStatus;
import com.jjh.kkokjaba.missingcase.domain.MissingCase;
import com.jjh.kkokjaba.missingcase.event.CaseChanged;
import com.jjh.kkokjaba.missingcase.event.CaseChanged.Kind;
import com.jjh.kkokjaba.missingcase.repository.MissingCaseRepository;

/**
 * 컨트롤러와 도메인 사이의 얇은 층.
 * 하는 일: 사건을 찾고, 도메인 메서드를 부르고, 트랜잭션으로 묶고, "바뀌었다"는 이벤트를 던진다.
 * 규칙(단계 건너뛰기 금지, 팔찌 확인)은 여기 없고 MissingCase 안에 있다.
 */
@Service
@Transactional(readOnly = true) // 기본은 읽기 전용, 바꾸는 메서드만 아래에서 따로 연다
public class MissingCaseService {

	private static final Set<CaseStatus> OPEN = EnumSet.complementOf(EnumSet.of(CaseStatus.HANDED_OVER));

	private final MissingCaseRepository repository;
	private final ApplicationEventPublisher events;

	public MissingCaseService(MissingCaseRepository repository, ApplicationEventPublisher events) {
		this.repository = repository;
		this.events = events;
	}

	public List<MissingCase> openCases() {
		return repository.findByStatusInOrderByCreatedAtDesc(OPEN);
	}

	public MissingCase get(Long id) {
		return repository.findById(id).orElseThrow(() -> new CaseNotFoundException(id));
	}

	@Transactional
	public MissingCase report(String guardianBand, String clothingColor, String lastSeenZone) {
		return publish(Kind.OPENED, repository.save(MissingCase.reportedByGuardian(guardianBand, clothingColor, lastSeenZone)));
	}

	@Transactional
	public MissingCase detect(String clothingColor, String zone) {
		return publish(Kind.OPENED, repository.save(MissingCase.detectedByCamera(clothingColor, zone)));
	}

	// 아래 메서드들은 save()를 부르지 않는다.
	// 트랜잭션 안에서 읽어 온 엔티티는 JPA가 지켜보고 있다가, 값이 바뀌면 커밋할 때 UPDATE를 알아서 보낸다(변경 감지).
	// 도메인 메서드가 예외를 던지면 publish까지 가지 않으므로, 실패한 요청은 알림도 나가지 않는다.

	//
	// expectedVersion: 직원 화면이 마지막으로 본 사건의 version. null이면 확인을 건너뛴다(curl 등으로 직접 부를 때).

	@Transactional
	public MissingCase linkGuardian(Long id, String guardianBand, Long expectedVersion) {
		MissingCase c = load(id, expectedVersion);
		c.linkGuardian(guardianBand);
		return publish(Kind.UPDATED, c);
	}

	@Transactional
	public MissingCase assignStaff(Long id, String staffName, Long expectedVersion) {
		MissingCase c = load(id, expectedVersion);
		c.assignStaff(staffName);
		return publish(Kind.UPDATED, c);
	}

	@Transactional
	public MissingCase markFound(Long id, String zone, Long expectedVersion) {
		MissingCase c = load(id, expectedVersion);
		c.markFound(zone);
		return publish(Kind.UPDATED, c);
	}

	@Transactional
	public MissingCase moveToShelter(Long id, Long expectedVersion) {
		MissingCase c = load(id, expectedVersion);
		c.moveToShelter();
		return publish(Kind.UPDATED, c);
	}

	@Transactional
	public MissingCase handOver(Long id, String presentedBand, Long expectedVersion) {
		MissingCase c = load(id, expectedVersion);
		c.handOver(presentedBand);
		return publish(Kind.UPDATED, c);
	}

	/**
	 * 직원 A가 1분 전 화면을 보고 버튼을 눌렀는데, 그사이 직원 B가 이미 사건을 바꿨다면 거절한다.
	 * @Version만으로는 "정확히 동시에" 들어온 두 요청만 잡히고, 이렇게 시간차가 있는 경우는 못 잡는다.
	 */
	private MissingCase load(Long id, Long expectedVersion) {
		MissingCase c = get(id);
		if (expectedVersion != null && !expectedVersion.equals(c.getVersion())) {
			throw new OptimisticLockingFailureException(id + "번 사건이 화면을 연 뒤에 바뀌었습니다.");
		}
		return c;
	}

	/** 지금 바로 보내는 게 아니라 "커밋되면 보내 달라"고 맡겨 두는 것. 실제 전송은 CaseEventBroadcaster가 커밋 후에 한다. */
	private MissingCase publish(Kind kind, MissingCase c) {
		events.publishEvent(new CaseChanged(kind, c));
		return c;
	}
}
