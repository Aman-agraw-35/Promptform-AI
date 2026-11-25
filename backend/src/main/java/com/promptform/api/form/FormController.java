package com.promptform.api.form;

import jakarta.validation.Valid;
import java.util.UUID;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/forms")
public class FormController {
    private final FormService formService;

    public FormController(FormService formService) { this.formService = formService; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FormModels.FormResponse approve(@Valid @RequestBody FormModels.ApproveFormRequest request) {
        return formService.approve(request);
    }

    @GetMapping("/{formId}")
    public FormModels.FormResponse get(@PathVariable UUID formId) {
        return formService.get(formId);
    }

    @GetMapping("/{formId}/submissions")
    public List<FormModels.SubmissionDetails> submissions(@PathVariable UUID formId) {
        return formService.listSubmissions(formId);
    }

    @PostMapping("/{formId}/submissions")
    @ResponseStatus(HttpStatus.CREATED)
    public FormModels.SubmissionResponse submit(
            @PathVariable UUID formId,
            @Valid @RequestBody FormModels.CreateSubmissionRequest request) {
        return formService.submit(formId, request);
    }
}
