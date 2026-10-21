import {
  LegalDocument,
  LegalList,
  LegalParagraph,
  LegalSection,
} from "@/features/legal/components/legal-document";

export const metadata = {
  title: "Términos y condiciones | SuperFercho",
};

export default function TermsPage() {
  return (
    <LegalDocument
      title="Términos y condiciones"
      updatedAt="17 de octubre de 2026"
      intro="Estos términos describen las condiciones aplicables al usar la tienda en línea de SuperFercho. Es una versión inicial de ejemplo: antes de su publicación definitiva debe ser revisada y completada con la información del responsable del servicio."
      other={{ href: "/legal/privacy", label: "Ver Política de privacidad" }}
    >
      <LegalSection title="1. Uso de la tienda y de la cuenta">
        <LegalParagraph>
          Para comprar debes crear una cuenta con información veraz y mantener
          la confidencialidad de tus credenciales. Las acciones realizadas
          desde tu sesión se asocian a tu cuenta. La tienda es para uso
          personal: no debes usarla con fines ilícitos ni alterar su
          funcionamiento.
        </LegalParagraph>
      </LegalSection>

      <LegalSection title="2. Productos, precios y disponibilidad">
        <LegalParagraph>
          Los productos, precios y disponibilidades se muestran en el catálogo.
          Los precios y la disponibilidad se verifican durante el proceso de
          pedido: si un precio cambió o un producto no está disponible, el
          pedido no se confirma y se te informa para que lo revises.
        </LegalParagraph>
      </LegalSection>

      <LegalSection title="3. Métodos de pago">
        <LegalList>
          <li>
            Tarjeta simulada: es una simulación de pago; no se solicitan ni se
            almacenan datos reales de tarjeta.
          </li>
          <li>Pago contra entrega: el pago queda pendiente hasta la entrega.</li>
        </LegalList>
      </LegalSection>

      <LegalSection title="4. Domicilio">
        <LegalParagraph>
          El costo del domicilio se acuerda directamente con el domiciliario y
          no está incluido en el total de los productos que muestra
          SuperFercho.
        </LegalParagraph>
      </LegalSection>

      <LegalSection title="5. Cancelación del pedido">
        <LegalParagraph>
          Puedes cancelar tu pedido desde el detalle del pedido durante los
          primeros dos minutos posteriores a su confirmación, de acuerdo con el
          funcionamiento actual de la tienda. Vencido ese tiempo, el pedido
          continúa su proceso.
        </LegalParagraph>
      </LegalSection>

      <LegalSection title="6. Reclamaciones y situaciones no descritas">
        <LegalParagraph>
          Cualquier reclamación, devolución, garantía o situación no descrita en
          este documento se atenderá conforme a la normativa aplicable en
          materia de protección al consumidor y a las condiciones que
          SuperFercho defina para el servicio.
        </LegalParagraph>
      </LegalSection>
    </LegalDocument>
  );
}
