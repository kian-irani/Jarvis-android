# Discovery UX — Design Spec (2026-06-11)

## Goal
Complete Brain Discovery user experience on top of the finished backend
(JoinPayload eb122f7, NSD 94fb20c, wizard scan+handshake 81c7073).

## Components
1. **BrainSelectionStore** (`brain/discovery/`): interface `save(JoinPayload)` / `load(): JoinPayload?`;
   DataStore-preferences impl; Hilt-provided. Consumers: SetupWizardViewModel (save on
   ConnectStatus.OK), BrainLiteService (HeartbeatSender.brainBaseUrl = stored brain ?: localhost).
2. **Candidates list UI** (SetupWizardScreen, Discovery step, method=MDNS): HUD-themed list of
   state.candidates (name + host:port), tap → onCandidateSelected; empty state = scanning radar
   animation. Design per ui-ux-pro-max rule — no generic Material.
3. **Retry on FAILED**: connect step shows error banner + RETRY button (calls next()).
4. **QR scan** (method=QR): CameraX + ML Kit barcode; on decode → onTokenChanged(raw) (existing
   decode path). Camera permission denied → auto-switch to TOKEN method with hint.
5. **QR generator** (brain side): QrPairingScreen renders JoinPayload(host=local wifi IP, 7799,
   token from prefs) via zxing; reachable from HUD menu.

## Error handling
- Handshake failure → FAILED + retry (done at VM level in 81c7073).
- No candidates after 15s → show "no brains found" + switch-method hint.

## Testing
- JVM: BrainSelectionStore round-trip; wizard retry path; QR payload generation logic.
- Instrumented-guarded: camera/scan, DataStore on device.

## Out of scope (YAGNI)
- Multi-brain management UI (Election screen already covers it)
- Token rotation/expiry (Phase 2 security)
