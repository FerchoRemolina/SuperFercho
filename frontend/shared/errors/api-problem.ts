export type ApiProblem = {
  status: number;
  code?: string;
  title?: string;
  detail?: string;
};

export class ApiError extends Error {
  readonly problem: ApiProblem;

  constructor(problem: ApiProblem) {
    super(problem.detail ?? problem.title ?? "API error");
    this.name = "ApiError";
    this.problem = problem;
  }
}

export function isApiError(error: unknown): error is ApiError {
  return error instanceof ApiError;
}

export function parseApiProblem(status: number, body: unknown): ApiProblem {
  if (!isRecord(body)) {
    return { status };
  }

  return {
    status:
      typeof body.status === "number" ? body.status : status,
    code: optionalString(body.code),
    title: optionalString(body.title),
    detail: optionalString(body.detail),
  };
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null;
}

function optionalString(value: unknown): string | undefined {
  return typeof value === "string" && value.length > 0 ? value : undefined;
}
