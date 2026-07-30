'use strict';

(function() {
    const usageSeries = Array.from(document.querySelectorAll('[data-usage-point]'))
        .map(function(point) {
            return {
                date: point.dataset.date || '',
                value: point.dataset.value || null,
                pct: readOptionalNumber(point.dataset.pct),
                usedTb: readOptionalNumber(point.dataset.usedTb),
                sizeTb: readOptionalNumber(point.dataset.sizeTb)
            };
        });

    function readOptionalNumber(raw) {
        if (raw == null || String(raw).trim() === '') return null;
        const value = Number(String(raw).replace(/,/g, ''));
        return Number.isFinite(value) ? value : null;
    }

    if (!usageSeries || usageSeries.length === 0) return;

    // 퍼센트 문자열/숫자 파싱 (예: "75", "75%", "75.2 %") - 레거시 value 대비용
    function parsePercent(x) {
        if (x == null) return null;
        const raw = String(x).trim();
        if (!raw) return null;
        const cleaned = raw.replace(/,/g, '').replace(/[^0-9.\-]/g, '');
        if (!cleaned || cleaned === '-' || cleaned === '.') return null;
        const v = Number(cleaned);
        return Number.isFinite(v) ? v : null;
    }

    // 차트 X축 라벨과 데이터 구성 (오름차순 가정)
    const labels = usageSeries.map(p => p.date);
    const usageData = usageSeries.map(p => Number.isFinite(p.pct) ? p.pct : parsePercent(p.value));
    const usedSizeData = usageSeries.map(p => Number.isFinite(p.usedTb) ? p.usedTb : null);
    const capacitySizeData = usageSeries.map(p => Number.isFinite(p.sizeTb) ? p.sizeTb : null);

    const ctx = document.getElementById('licenseUsageChart');
    if (!ctx || typeof window.Chart !== 'function') return;

    new window.Chart(ctx, {
        type: 'line',
        data: {
            labels,
            datasets: [
                {
                    label: '사용률(%)',
                    data: usageData,
                    yAxisID: 'y',
                    borderColor: '#3b82f6',
                    backgroundColor: 'rgba(59,130,246,0.15)',
                    tension: 0.25,
                    spanGaps: true,
                    pointRadius: 3,
                    pointHoverRadius: 5,
                    fill: false
                },
                {
                    label: '라이선스 사용량(TB)',
                    data: usedSizeData,
                    yAxisID: 'y1',
                    borderColor: '#10b981',
                    backgroundColor: 'rgba(16,185,129,0.15)',
                    tension: 0,
                    stepped: true,
                    spanGaps: true,
                    pointRadius: 2,
                    pointHoverRadius: 4,
                    fill: false
                },
                {
                    label: '라이선스 크기(TB)',
                    data: capacitySizeData,
                    yAxisID: 'y1',
                    borderColor: '#ef4444',
                    backgroundColor: 'rgba(239,68,68,0.15)',
                    tension: 0,
                    stepped: true,
                    pointRadius: 2,
                    pointHoverRadius: 4,
                    fill: false
                }
            ]
        },
        options: {
            responsive: true,
            interaction: { mode: 'index', intersect: false },
            plugins: {
                legend: { position: 'top' },
                tooltip: {
                    // 숫자 값이 있는 항목만 노출
                    filter: function(context) {
                        const y = context && context.parsed ? context.parsed.y : null;
                        return y != null && Number.isFinite(y);
                    },
                    callbacks: {
                        // 점검일(라벨) 표시
                        title: function(items) {
                            if (!items || !items.length) return '';
                            const idx = items[0].dataIndex;
                            return '점검일: ' + (Array.isArray(labels) ? labels[idx] : '');
                        },
                        // 값 + 단위 정확 표기
                        label: function(ctx) {
                            let y = ctx && ctx.parsed ? ctx.parsed.y : null;
                            if (y == null || !Number.isFinite(y)) {
                                // 보강: raw에서도 숫자 시도
                                const raw = ctx.raw;
                                const cand = raw && typeof raw === 'object' ? (raw.y ?? raw.value) : raw;
                                if (cand != null) {
                                    const n = (typeof cand === 'number') ? cand : Number(String(cand).replace(/,/g,'').replace(/[^0-9.\-]/g,''));
                                    if (Number.isFinite(n)) y = n;
                                }
                            }
                            if (y == null || !Number.isFinite(y)) return null;
                            const name = ctx.dataset && ctx.dataset.label ? ctx.dataset.label : '';
                            if (ctx.dataset && ctx.dataset.yAxisID === 'y') {
                                return (name ? name + ': ' : '') + y.toFixed(0) + '%';
                            }
                            return (name ? name + ': ' : '') + y.toFixed(2) + ' TB';
                        }
                    }
                }
            },
            scales: {
                y: {
                    type: 'linear',
                    position: 'left',
                    suggestedMin: 0,
                    suggestedMax: 100,
                    ticks: {
                        callback: function(v) {
                            const n = Number(v);
                            return Number.isFinite(n) ? n.toFixed(0) : String(v);
                        }
                    },
                    title: { display: true, text: '사용률(%)' }
                },
                y1: {
                    type: 'linear',
                    position: 'right',
                    min: 0,
                    grid: { drawOnChartArea: false },
                    title: { display: true, text: '용량(TB)' }
                },
                x: { title: { display: true, text: '점검일' } }
            }
        }
    });
})();

document.addEventListener('DOMContentLoaded', function() {
    // 히스토리 아이템 애니메이션
    const historyItems = document.querySelectorAll('.history-item');
    const staggerStep = historyItems.length > 20 ? 15 : 40;
    const maxDelay = 240;

    historyItems.forEach((item, index) => {
        item.addEventListener('click', function() {
            window.location.href = this.dataset.detailUrl;
        });
        if (typeof item.animate === 'function') {
            item.animate(
                [
                    { opacity: 0, transform: 'translateY(20px)' },
                    { opacity: 1, transform: 'translateY(0)' }
                ],
                {
                    duration: 500,
                    delay: Math.min(index * staggerStep, maxDelay),
                    easing: 'ease',
                    fill: 'both'
                });
        }
    });
});
