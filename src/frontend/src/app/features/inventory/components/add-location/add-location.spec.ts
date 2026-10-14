import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { Signal, WritableSignal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { AddLocationService } from '../../services/add-location-service';
import { AddLocation } from './add-location';

// typed view for testing
type TestableAddLocation = AddLocation & {
  locationModel: WritableSignal<any>;
  locationResource: { reaload: () => void };
  createError: WritableSignal<string | null>;
  editingId: Signal<number | null>;
  editCode: Signal<string>;
  updateError: WritableSignal<string | null>;
  updateMessage: WritableSignal<string | null>;
  locationForm: any; // form singals API
};
describe('AddLocation', () => {
  let component: TestableAddLocation;
  let fixture: ComponentFixture<AddLocation>;
  let mockLocationService: any;

  beforeEach(async () => {
    mockLocationService = {
      locationResource: { reload: vi.fn() },
      createLocation: vi.fn(),
      updateLocation: vi.fn(),
      deleteLocation: vi.fn(),
    };

    await TestBed.configureTestingModule({
      imports: [AddLocation],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: AddLocationService, useValue: mockLocationService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(AddLocation);
    component = fixture.componentInstance as unknown as TestableAddLocation;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('Edit Actions', () => {
    it('should set editing state on onEdit', () => {
      // setup
      const mockLocation = { id: 1, locationCode: 'LOC-01' };
      // act
      component.onEdit(mockLocation);
      // assert
      expect(component.editingId()).toBe(1);
      expect(component.editCode()).toBe('LOC-01');
    });
    it('should reset editing state on onCancelEdit', () => {
      // setup
      component.editingId.set(1);
      component.editCode.set('LOC-01');
      // act
      component.onCancelEdit();
      expect(component.editingId()).toBeNull();
      expect(component.editCode()).toBe('');
    });
    it('should update editCode signal when user types in input field', async () => {
      // setup
      // fixture.detectChanges();
      // const input = fixture.nativeElement.querySelector('input[data-testid="edit-code-input"]');
      // // expect(input).not.toBeNull();
      // // input.value = 'NEW-CODE-123';
      // // input.dispatchEvent(new Event('input'));
      // // act
      // // fixture.detectChanges();
      // // await fixture.whenStable();
      // // assert
      // // expect(component.editCode()).toBe('NEW-CODE-123');
      // // TODO  need to fix error !!!
      // expect(false).toBe(true);
    });

    it('should save edit state on onSaveEdit', () => {
      component.editCode.set('');
      component.onSaveEdit(1);
      expect(component.updateError()).toBe("Location code can't be empty");
      expect(mockLocationService.updateLocation).not.toHaveBeenCalled();
    });

    it('should handle successful and reaload resource', () => {
      // setup
      component.editCode.set('NEW-CODE');
      mockLocationService.updateLocation.mockReturnValue(of('Location NEW-CODE updated'));
      // act
      component.onSaveEdit(1);
      // assert
      expect(component.updateMessage()).toBe('Location NEW-CODE updated');
      expect(component.updateError()).toBe(null);
      expect(mockLocationService.locationResource.reload).toHaveBeenCalled();
      expect(component.editingId()).toBeNull();
    });

    it('should handle various error types in updateLocation', () => {
      component.editCode.set('BAD-CODE');
      // Test 1: JSON error z wiadomością
      mockLocationService.updateLocation.mockReturnValue(
        throwError(() => ({ error: JSON.stringify({ message: 'Server error' }) })),
      );
      component.onSaveEdit(1);
      expect(component.updateError()).toBe('Server error');

      // Test 2: Plain text error
      mockLocationService.updateLocation.mockReturnValue(
        throwError(() => ({ error: 'Plain text error' })),
      );
      component.onSaveEdit(1);
      expect(component.updateError()).toBe('Plain text error');

      // Test 3: Obiekt bez wiadomości (fallback)
      mockLocationService.updateLocation.mockReturnValue(
        throwError(() => ({ error: { somethingElse: 'no message' } })),
      );
      component.onSaveEdit(1);
      expect(component.updateError()).toBe('Could not update location');
    });
  });

  describe('delete Locations', () => {
    const mockDeleteLocation = { id: 1, locationCode: 'CODE-01' };
    it('does nothing wehn confirm is rejected', () => {
      vi.spyOn(window, 'confirm').mockImplementation(() => false);
      component.onDeleteLocation(mockDeleteLocation);
      expect(mockLocationService.deleteLocation).not.toHaveBeenCalled();
    });

    it('delete and refreshes list on success', () => {
      vi.spyOn(window, 'confirm').mockImplementation(() => true);
      mockLocationService.deleteLocation.mockReturnValue(of('ok'));
      component.onDeleteLocation(mockDeleteLocation);
      expect(mockLocationService.deleteLocation).toHaveBeenCalledWith(1);
      expect(component.updateMessage()).toBe('ok');
      expect(mockLocationService.locationResource.reload).toHaveBeenCalled();
    });

    it('sets error message on failure', () => {
      vi.spyOn(window, 'confirm').mockImplementation(() => true);
      mockLocationService.deleteLocation.mockReturnValue(
        throwError(() => ({ error: JSON.stringify({ message: 'Server error' }) })),
      );
      component.onDeleteLocation(mockDeleteLocation);
      expect(component.updateError()).toBe('Server error');
    });
    it('sets error body message on failure', () => {
      vi.spyOn(window, 'confirm').mockImplementation(() => true);
      mockLocationService.deleteLocation.mockReturnValue(
        throwError(() => ({ error: JSON.stringify('Could not delete location') })),
      );
      component.onDeleteLocation(mockDeleteLocation);
      expect(component.updateError()).toBe('"Could not delete location"');
    });

    it('sets error object message on failure', () => {
      vi.spyOn(window, 'confirm').mockImplementation(() => true);
      mockLocationService.deleteLocation.mockReturnValue(
        throwError(() => ({
          error: { error: {} },
        })),
      );
      component.onDeleteLocation(mockDeleteLocation);
      expect(component.updateError()).toBe('Could not delete location');
    });

    it('sets error message from object property on failure', () => {
      vi.spyOn(window, 'confirm').mockImplementation(() => true);

      // Scenariusz: API zwraca obiekt bezpośrednio (bez JSON.stringify)
      // z właściwością .message (często spotykane w Axios/Fetch)
      mockLocationService.deleteLocation.mockReturnValue(
        throwError(() => ({
          error: { message: 'Validation failed' },
        })),
      );

      component.onDeleteLocation(mockDeleteLocation);
      expect(component.updateError()).toBe('Validation failed');
    });
  });

  describe('onSubmit tests', () => {
    const mockLocation = { id: 1, locationCode: ' LOC-1-01 ' };
    const mockFormObject = {
      invalid: vi.fn(),
      locationCode: {
        invalid: vi.fn(),
        touched: vi.fn(),
        errors: vi.fn(),
      },
      reset: vi.fn(),
    };
    const createMockEvent = () => ({ preventDefault: vi.fn() }) as any;

    beforeEach(async () => {
      component.locationForm = vi.fn().mockReturnValue(mockFormObject);
    });

    it('should return early when form is invalid', () => {
      mockFormObject.invalid.mockReturnValue(true);
      const event = createMockEvent();
      component.onSubmit(event);

      expect(event.preventDefault).toHaveBeenCalled();
      expect(mockLocationService.createLocation).not.toHaveBeenCalled();
    });

    it('should have successful createion with server message', () => {
      mockFormObject.invalid.mockReturnValue(false);
      const serverMsg = `Location successful created`;
      mockLocationService.createLocation.mockReturnValue(of(serverMsg));
      component.onSubmit(createMockEvent());

      // expect(component.locationModel()).toBe('LOC-1-01');
      // expect(mockLocationService.createLocation).toHaveBeenCalledWith(serverMsg);
      // expect(mockLocationService.createError).toHaveBeenCalledWith();
    });
  });

  afterEach(() => vi.restoreAllMocks());
});
