'use strict';

(function() {
    function initializeHistoryDisclosures() {
        document.addEventListener('click', function(event) {
            if (!(event.target instanceof Element)) return;
            const toggle = event.target.closest('[data-history-toggle]');
            if (!toggle) return;

            const detailId = toggle.getAttribute('aria-controls');
            const detail = detailId ? document.getElementById(detailId) : null;
            if (!detail) return;

            const expanded = toggle.getAttribute('aria-expanded') === 'true';
            toggle.setAttribute('aria-expanded', String(!expanded));
            detail.hidden = expanded;
            const nextLabel = expanded
                ? toggle.dataset.expandLabel
                : toggle.dataset.collapseLabel;
            if (nextLabel) toggle.setAttribute('aria-label', nextLabel);
        });
    }

    initializeHistoryDisclosures();

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
    const reduceMotion = typeof window.matchMedia === 'function'
        && window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    const compactChart = typeof window.matchMedia === 'function'
        && window.matchMedia('(max-width: 768px)').matches;

    const rootStyles = window.getComputedStyle(document.documentElement);
    function cssColor(tokenName) {
        const value = rootStyles.getPropertyValue(tokenName).trim();
        if (!value) throw new Error('Missing chart color token: ' + tokenName);
        return value;
    }

    const chartColors = {
        usage: cssColor('--color-chart-usage'),
        used: cssColor('--color-chart-used'),
        capacity: cssColor('--color-chart-capacity'),
        text: cssColor('--color-text'),
        textMuted: cssColor('--color-text-muted'),
        textStrong: cssColor('--color-text-strong'),
        surface: cssColor('--color-surface'),
        surfaceMuted: cssColor('--color-surface-muted'),
        border: cssColor('--color-border'),
        borderStrong: cssColor('--color-border-strong')
    };

    new window.Chart(ctx, {
        type: 'line',
        data: {
            labels,
            datasets: [
                {
                    label: '사용률(%)',
                    data: usageData,
                    yAxisID: 'y',
                    borderColor: chartColors.usage,
                    backgroundColor: chartColors.usage,
                    pointBackgroundColor: chartColors.usage,
                    pointBorderColor: chartColors.usage,
                    tension: 0.25,
                    spanGaps: true,
                    pointRadius: 3,
                    pointHoverRadius: 5,
                    pointStyle: 'circle',
                    fill: false
                },
                {
                    label: '라이선스 사용량(TB)',
                    data: usedSizeData,
                    yAxisID: 'y1',
                    borderColor: chartColors.used,
                    backgroundColor: chartColors.used,
                    pointBackgroundColor: chartColors.used,
                    pointBorderColor: chartColors.used,
                    tension: 0,
                    stepped: true,
                    spanGaps: true,
                    pointRadius: 2,
                    pointHoverRadius: 4,
                    pointStyle: 'triangle',
                    fill: false
                },
                {
                    label: '라이선스 크기(TB)',
                    data: capacitySizeData,
                    yAxisID: 'y1',
                    borderColor: chartColors.capacity,
                    backgroundColor: chartColors.capacity,
                    pointBackgroundColor: chartColors.capacity,
                    pointBorderColor: chartColors.capacity,
                    borderDash: [6, 4],
                    tension: 0,
                    stepped: true,
                    pointRadius: 2,
                    pointHoverRadius: 4,
                    pointStyle: 'rectRot',
                    fill: false
                }
            ]
        },
        options: {
            animation: reduceMotion ? false : undefined,
            responsive: true,
            interaction: { mode: 'index', intersect: false },
            plugins: {
                legend: {
                    position: 'top',
                    labels: { color: chartColors.text }
                },
                tooltip: {
                    backgroundColor: chartColors.textStrong,
                    titleColor: chartColors.surface,
                    bodyColor: chartColors.surface,
                    borderColor: chartColors.borderStrong,
                    borderWidth: 1,
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
                                return (name ? name + ': ' : '') + y.toFixed(1) + '%';
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
                        color: chartColors.textMuted,
                        callback: function(v) {
                            const n = Number(v);
                            return Number.isFinite(n) ? n.toFixed(0) : String(v);
                        }
                    },
                    grid: { color: chartColors.border },
                    title: { display: true, text: '사용률(%)', color: chartColors.text }
                },
                y1: {
                    type: 'linear',
                    position: 'right',
                    min: 0,
                    grid: { drawOnChartArea: false, color: chartColors.border },
                    ticks: { color: chartColors.textMuted },
                    title: { display: true, text: '용량(TB)', color: chartColors.text }
                },
                x: {
                    grid: { color: chartColors.border },
                    ticks: {
                        color: chartColors.textMuted,
                        maxRotation: compactChart ? 0 : 50,
                        maxTicksLimit: compactChart ? 8 : undefined,
                        minRotation: 0
                    },
                    title: { display: true, text: '점검일', color: chartColors.text }
                }
            }
        }
    });
})();
