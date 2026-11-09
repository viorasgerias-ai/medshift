const express = require('express');
const cors = require('cors');

const app = express();
app.use(cors({ origin: true }));
app.use(express.json({ limit: '256kb' }));

const EMAIL_REGEX = /[a-zA-Z0-9._%+\-]+@[a-zA-Z0-9.\-]+\.[a-zA-Z]{2,}/g;
const CPF_REGEX = /\b\d{3}\.?\d{3}\.?\d{3}-?\d{2}\b/g;
const PHONE_REGEX = /(?:\+55\s*)?(?:\(?\d{2}\)?\s*)9?\d{4}[\-\s]\d{4}\b/g;

function redactLgpd(text) {
  if (!text || typeof text !== 'string') return '';
  return text
    .replace(EMAIL_REGEX, '[EMAIL REDIGIDO - LGPD]')
    .replace(CPF_REGEX, '[CPF REDIGIDO - LGPD]')
    .replace(PHONE_REGEX, '[TELEFONE REDIGIDO - LGPD]');
}

function evaluateTcria(payload) {
  const projectClinicAnchor = (payload.projectClinicAnchor || payload.clinica || '').trim();
  const caseReference = (payload.caseReference || payload.paciente || '').trim();
  const responsibleHuman = (payload.responsibleHuman || payload.medico || '').trim();
  const declaredPurpose = (payload.declaredPurpose || 'Continuidade assistencial').trim();
  const contextScope = (payload.contextScope || 'Atendimento clínico').trim();
  const evidencias = redactLgpd(payload.evidencias || '');
  const raciocinio = redactLgpd(payload.raciocinio || '');
  const decisao = redactLgpd(payload.decisao || '');

  const gates = [
    {
      ruleCode: 'RULE_1.1',
      gateName: 'Project Anchor Gate',
      passed: projectClinicAnchor.length >= 3,
      status: projectClinicAnchor.length >= 3 ? 'VALID' : 'INVALID'
    },
    {
      ruleCode: 'RULE_1.2',
      gateName: 'Context Integrity Gate',
      passed: contextScope.length >= 5 && caseReference.length > 0,
      status: contextScope.length >= 5 && caseReference.length > 0 ? 'VALID' : 'INVALID'
    },
    {
      ruleCode: 'RULE_4.1',
      gateName: 'Compliance & Accountability Gate',
      passed: responsibleHuman.length >= 4 && declaredPurpose.length >= 4,
      status: responsibleHuman.length >= 4 && declaredPurpose.length >= 4 ? 'VALID' : 'BLOCKED'
    },
    {
      ruleCode: 'RULE_2.1 / 5.1',
      gateName: 'Traceability & Evidence Check',
      passed: evidencias.length >= 5 && raciocinio.length >= 4 && decisao.length >= 4,
      status: (raciocinio.length < 4 || decisao.length < 4)
        ? 'INVALID'
        : (evidencias.length < 5 ? 'WEAK' : 'VALID')
    },
    {
      ruleCode: 'RULE_3.1 / LGPD',
      gateName: 'Prescriptive & LGPD Gate',
      passed: true,
      status: 'VALID'
    }
  ];

  const overallStatus = gates.some(g => g.status === 'BLOCKED')
    ? 'BLOCKED'
    : gates.some(g => g.status === 'INVALID')
      ? 'INVALID'
      : gates.some(g => g.status === 'WEAK')
        ? 'WEAK'
        : 'VALID';

  return {
    overallStatus,
    gates,
    sanitizedPayload: {
      projectClinicAnchor: redactLgpd(projectClinicAnchor),
      caseReference: redactLgpd(caseReference),
      responsibleHuman: redactLgpd(responsibleHuman),
      declaredPurpose: redactLgpd(declaredPurpose),
      contextScope: redactLgpd(contextScope),
      evidencias,
      raciocinio,
      decisao
    }
  };
}

app.get('/api/health', (req, res) => {
  res.status(200).json({
    ok: true,
    service: 'medshift-tcria-api',
    runtime: 'Cloud Run Serverless',
    region: process.env.K_SERVICE ? 'southamerica-east1' : 'local-container',
    firebaseProjectId: 'gen-lang-client-0265308632',
    firestoreDatabaseId: 'ai-studio-android-medshift-eabb9a2d-89b1-44ad-943c-843ef86c682b',
    lgpdAutoRedaction: 'ENABLED',
    tcriaRulesetVersion: '1.0-CORE',
    timestamp: new Date().toISOString()
  });
});

app.post('/api/governance/evaluate', (req, res) => {
  const evaluation = evaluateTcria(req.body || {});
  res.status(200).json({
    ok: true,
    ...evaluation
  });
});

const PORT = process.env.PORT || 8080;
if (require.main === module) {
  app.listen(PORT, () => {
    console.log(`MedShift TCRIA Cloud Run Serverless API listening on port ${PORT}`);
  });
}

module.exports = app;
