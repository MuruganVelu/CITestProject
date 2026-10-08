package com.mrgn.knowledge.generate;

import com.mrgn.knowledge.domain.KnowledgeArtifact;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class HeuristicInductionGenerator {

    public String generate(StoryTestLinker.Coverage coverage, String generatorLabel) {
        StringBuilder md = new StringBuilder();
        md.append("# Internet Banking — Induction & Onboarding Guide\n\n");
        md.append("_Generated ").append(Instant.now()).append(" by ").append(generatorLabel).append("._\n\n");
        md.append("This pack is derived from uploaded **Jira stories** and the **regression master**. ");
        md.append("It is the first-week reading list for engineers, testers, and automation developers.\n\n");

        appendOverview(md, coverage);
        appendStoryWalkthrough(md, coverage);
        appendRegressionLandscape(md, coverage);
        appendOnboardingChecklist(md, coverage);
        appendAutomationContext(md, coverage);
        return md.toString();
    }

    private void appendOverview(StringBuilder md, StoryTestLinker.Coverage coverage) {
        md.append("## 1. Application overview\n\n");
        md.append("The application is an **internet banking** channel. Customers authenticate, view accounts, ");
        md.append("move money (own-account and beneficiary transfers), pay bills, manage payees, download statements, ");
        md.append("operate cards, observe limits, and receive alerts. The corpus below is the functional source of truth ");
        md.append("until a live Jira project is connected.\n\n");
        md.append("| Metric | Count |\n| --- | ---: |\n");
        md.append("| Jira stories | ").append(coverage.linked().size()).append(" |\n");
        long uniqueTests = java.util.stream.Stream.concat(
                        coverage.linked().stream().flatMap(l -> l.tests().stream()),
                        coverage.orphanTests().stream())
                .map(KnowledgeArtifact::getExternalId)
                .filter(id -> id != null && !id.isBlank())
                .distinct()
                .count();
        md.append("| Regression test cases | ").append(uniqueTests).append(" |\n");
        md.append("| Stories with no linked tests | ").append(coverage.uncoveredStories().size()).append(" |\n");
        md.append("| Tests with no linked story | ").append(coverage.orphanTests().size()).append(" |\n\n");

        md.append("### Capability map (from stories)\n\n");
        for (StoryTestLinker.LinkedStory linked : sortedStories(coverage)) {
            md.append("- **").append(nz(linked.story().getExternalId())).append("** — ")
                    .append(nz(linked.story().getTitle()))
                    .append(" (").append(linked.tests().size()).append(" regression cases)\n");
        }
        md.append('\n');
    }

    private void appendStoryWalkthrough(StringBuilder md, StoryTestLinker.Coverage coverage) {
        md.append("## 2. Jira story walkthrough\n\n");
        for (StoryTestLinker.LinkedStory linked : sortedStories(coverage)) {
            KnowledgeArtifact story = linked.story();
            md.append("### ").append(nz(story.getExternalId())).append(" — ").append(nz(story.getTitle())).append("\n\n");
            md.append(nz(story.getContent())).append("\n\n");
            if (linked.tests().isEmpty()) {
                md.append("> Coverage gap: no regression cases reference this story.\n\n");
            } else {
                md.append("**Linked regression cases**\n\n");
                for (KnowledgeArtifact test : linked.tests().stream()
                        .sorted(Comparator.comparing(a -> nz(a.getExternalId())))
                        .toList()) {
                    md.append("- `").append(nz(test.getExternalId())).append("` ").append(nz(test.getTitle())).append('\n');
                }
                md.append('\n');
            }
        }
    }

    private void appendRegressionLandscape(StringBuilder md, StoryTestLinker.Coverage coverage) {
        md.append("## 3. Regression landscape\n\n");
        Map<String, List<KnowledgeArtifact>> byArea = new LinkedHashMap<>();
        coverage.linked().stream()
                .flatMap(l -> l.tests().stream())
                .forEach(test -> byArea.computeIfAbsent(areaOf(test), k -> new java.util.ArrayList<>()).add(test));
        coverage.orphanTests().forEach(test -> byArea.computeIfAbsent(areaOf(test), k -> new java.util.ArrayList<>()).add(test));

        md.append("| Area | Cases |\n| --- | ---: |\n");
        byArea.forEach((area, tests) -> md.append("| ").append(area).append(" | ").append(tests.size()).append(" |\n"));
        md.append('\n');

        md.append("### Case catalogue\n\n");
        coverage.linked().stream()
                .flatMap(l -> l.tests().stream())
                .collect(Collectors.toMap(KnowledgeArtifact::getExternalId, t -> t, (a, b) -> a, LinkedHashMap::new))
                .values()
                .stream()
                .sorted(Comparator.comparing(a -> nz(a.getExternalId()), this::testIdOrder))
                .forEach(test -> {
                    md.append("#### ").append(nz(test.getExternalId())).append(" — ").append(nz(test.getTitle())).append("\n\n");
                    md.append(nz(test.getContent())).append("\n\n");
                });

        if (!coverage.orphanTests().isEmpty()) {
            md.append("### Unlinked regression cases\n\n");
            for (KnowledgeArtifact test : coverage.orphanTests()) {
                md.append("- `").append(nz(test.getExternalId())).append("` ").append(nz(test.getTitle())).append('\n');
            }
            md.append('\n');
        }
        if (!coverage.uncoveredStories().isEmpty()) {
            md.append("### Stories missing regression coverage\n\n");
            for (KnowledgeArtifact story : coverage.uncoveredStories()) {
                md.append("- `").append(nz(story.getExternalId())).append("` ").append(nz(story.getTitle())).append('\n');
            }
            md.append('\n');
        }
    }

    private void appendOnboardingChecklist(StringBuilder md, StoryTestLinker.Coverage coverage) {
        md.append("## 4. First-week onboarding checklist\n\n");
        md.append("1. Read every story in section 2 in order (auth → accounts → money movement → servicing).\n");
        md.append("2. Walk the happy-path regression cases for login, account summary, own-account transfer, bill pay, and statements.\n");
        md.append("3. Re-run the negative pack: lockout, insufficient funds, limit breach, invalid beneficiary, expired OTP.\n");
        md.append("4. Note coverage gaps and ask an SME before automating those stories.\n");
        md.append("5. Confirm dual-control / cool-off rules on beneficiaries and high-value transfers with operations.\n\n");
        md.append("Suggested read order:\n\n");
        int i = 1;
        for (StoryTestLinker.LinkedStory linked : sortedStories(coverage)) {
            md.append(i++).append(". `").append(nz(linked.story().getExternalId())).append("` ")
                    .append(nz(linked.story().getTitle())).append('\n');
        }
        md.append('\n');
    }

    private void appendAutomationContext(StringBuilder md, StoryTestLinker.Coverage coverage) {
        md.append("## 5. Automation context\n\n");
        md.append("Automate in this order (highest regression value, stable oracles):\n\n");
        md.append("1. Login success / fail / lockout and session timeout.\n");
        md.append("2. Account summary balances vs ledger-style expected values in the master.\n");
        md.append("3. Own-account transfer and beneficiary transfer with limit and insufficient-funds negatives.\n");
        md.append("4. Bill pay + add biller.\n");
        md.append("5. Statement download and card block/unblock.\n");
        md.append("6. Alert opt-in/out last (channel-dependent).\n\n");
        md.append("Prefer UI or API cases that already name a story key and expected result in the regression master.\n");
    }

    private List<StoryTestLinker.LinkedStory> sortedStories(StoryTestLinker.Coverage coverage) {
        return coverage.linked().stream()
                .sorted(Comparator.comparing(l -> nz(l.story().getExternalId())))
                .toList();
    }

    private String areaOf(KnowledgeArtifact test) {
        String text = (nz(test.getTitle()) + " " + nz(test.getContent())).toLowerCase();
        if (text.contains("login") || text.contains("otp") || text.contains("mfa") || text.contains("session") || text.contains("password") || text.contains("lock")) {
            return "Login / auth";
        }
        if (text.contains("beneficiar") || text.contains("payee")) {
            return "Beneficiaries";
        }
        if (text.contains("bill")) {
            return "Bill pay";
        }
        if (text.contains("transfer") || text.contains("funds") || text.contains("insufficient")) {
            return "Funds transfer";
        }
        if (text.contains("statement")) {
            return "Statements";
        }
        if (text.contains("card") || text.contains("pin")) {
            return "Cards";
        }
        if (text.contains("limit")) {
            return "Limits";
        }
        if (text.contains("alert") || text.contains("notif")) {
            return "Alerts";
        }
        if (text.contains("account") || text.contains("balance") || text.contains("summary")) {
            return "Account summary";
        }
        return "Other";
    }

    private int testIdOrder(String a, String b) {
        return Integer.compare(testNum(a), testNum(b));
    }

    private int testNum(String id) {
        try {
            return Integer.parseInt(id.replaceAll("\\D+", ""));
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private static String nz(String value) {
        return value == null ? "" : value;
    }
}
